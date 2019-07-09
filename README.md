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

## Programmatic use

    
## Copyright
Copyright (C) 2019 Crown Copyright (Office for National Statistics)

