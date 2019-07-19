# Census Event Generator
This is a utility spring boot web app, whose purpose is to create and publish rabbit amqp census events, for the purposes of 
performance testing.
The web app needs to be deployed to a GCP project where it will have access to the Rabbit instance it will publish to.
It's endpoint can then be called remotely, to instruct it to generate events.

The app is deployed using a k8s manifest, through which the location of the rabbit it sends to can
be configured.

The application has within its src/main/resources/template folder, json templates for each of the EventType
payloads. This is used as the pro-forma for the specific payload type, and values within that are modified according
to the contextual data sent to the endpoint.

## Endpoint
### POST /generate

This endpoint publishes events of the specified type.

Example command line usage:

```
    cat > /tmp/uac_updated.json <<EOF
    {
        "eventType": "UAC_UPDATED",
        "source": "SAMPLE_LOADER",
        "channel": "RM",
        "contexts": [
            {
                "uacHash": "147eb9dcde0e090429c01dbf634fd9b69a7f141f005c387a9c00498908499dde",
                "caseId": "f868fcfc-7280-40ea-ab01-b173ac245da3"
            }
        ]
    }
    EOF

    http --auth generator:hitmeup POST "http://localhost:8171/generate" @/tmp/uac_updated.json
```


Out of the box, basic auth :
user: generator, password: hitmeup
(Auth settings can be environment specific)

#### Request Body
```
{
	"eventType": "CASE_CREATED",
	"source": "RESPONDENT_HOME",
	"channel": "RH",
	"contexts": [
		{
			"caseRef": "hello",
			"id": "#uuid"
		},
		{
			"caseRef": "bar",
			"id": "#uuid"
		}
	]
}
```

####Response
```
{
    "payloads": [
        {
            "id": "06d33e2e-9c68-4f46-88d8-ce46c3abccc2",
            "caseRef": "hello",
            "survey": "census",
            "collectionExerciseId": "A1",
            "address": {
                "addressLine1": "2A Priors Way",
                "addressLine2": null,
                "addressLine3": null,
                "townName": null,
                "postcode": null,
                "region": null,
                "latitude": null,
                "longitude": null,
                "uprn": null,
                "arid": null,
                "addressType": null,
                "estabType": null
            },
            "contact": {
                "title": "Sir",
                "forename": "Phil",
                "surname": "Whiles",
                "email": "phil.whiles@gmail.com",
                "telNo": "07968583119"
            },
            "state": "ACTIVE",
            "actionableFrom": null
        },
        {
            "id": "0d1f7bc0-a964-48c5-be8a-5c0af359d0b9",
            "caseRef": "bar",
            "survey": "census",
            "collectionExerciseId": "A1",
            "address": {
                "addressLine1": "2A Priors Way",
                "addressLine2": null,
                "addressLine3": null,
                "townName": null,
                "postcode": null,
                "region": null,
                "latitude": null,
                "longitude": null,
                "uprn": null,
                "arid": null,
                "addressType": null,
                "estabType": null
            },
            "contact": {
                "title": "Sir",
                "forename": "Phil",
                "surname": "Whiles",
                "email": "phil.whiles@gmail.com",
                "telNo": "07968583119"
            },
            "state": "ACTIVE",
            "actionableFrom": null
        }
    ]
}
```

### GET /firestore/wait

This endpoint waits for an object to be created in Firestore. If the object is found before the timeout expires then it returns with a 200 status. If the object is not found in time then it returns a 404 (not found) status. 

The endpoint has 3 mandatory query parameters:
  - **collection**, this holds the name of the collection that we expect the object to be created in.
  - **key**, is the primary key for the object that are waiting for.
  - **timeout**, is the maximum time that we are prepared to wait for the object to appear. This supports units of milliseconds(ms) or seconds(s), eg 'timeout=250ms', 'timeout=2s' or 'timeout=2.5s'

Example command line invocation using Httpie:

```
http --auth generator:hitmeup  get "http://localhost:8171/firestore/wait?collection=case&key=f868fcfc-7280-40ea-ab01-b173ac245da3&timeout=500ms"

# Alternative
http --auth generator:hitmeup get http://localhost:8171/firestore/wait collection==case key==f868fcfc-7280-40ea-ab01-b173ac245da3 timeout==500ms
```


## Programmatic use

    
## Copyright
Copyright (C) 2019 Crown Copyright (Office for National Statistics)

