# Census Event Generator
This is a utility spring boot web app, whose purpose is to create and publish rabbit amqp census events, for the purposes of
performance testing.
The web app needs to be deployed to a GCP project where it will have access to the Rabbit instance it will publish to.
Its endpoint can then be called remotely, to instruct it to generate events.


The app is deployed using a k8s manifest, through which the location of the rabbit it sends to can
be configured.

The application has within its src/main/resources/template folder, json templates for each of the EventType
payloads. This is used as the pro-forma for the specific payload type, and values within that are modified according
to the contextual data sent to the endpoint.

## Endpoint

### SETUP

```
export EVENT_GEN_USER="** TBD **"
export EVENT_GEN_PASSWORD="** TBD **"

# point at local event generator
export EVENT_GEN_URL="http://localhost:8171"
# or point at census-rh-dev
export EVENT_GEN_URL="https://gen-dev.int.census-gcp.onsdigital.uk"
```


### POST /generate

This endpoint publishes events of the specified type.

Example command line usage:

```
cat > /tmp/uac_updated_01.json <<EOF
{
    "eventType": "UAC_UPDATED",
    "source": "SAMPLE_LOADER",
    "channel": "RM",
    "contexts": [
        {
            "uacHash": "a373eb6a658ab7ec2bc5eb24ddff2e40082db02a49bf9d35e7695f0705ba3a41",
            "active": true,
            "questionnaireId": "111111111",
            "caseId": "014477d1-dd3f-4c69-b181-7ff725dc9f01",
            "formType": "H"
        }
    ]
}
EOF

curl -s --data @/tmp/uac_updated_01.json -H "Content-Type: application/json" --user $EVENT_GEN_USER:$EVENT_GEN_PASSWORD $EVENT_GEN_URL/generate | jq
```

Out of the box, basic auth :
user: generator, password: hitmeup
(Auth settings can be environment specific)

#### Request Body
```
cat > /tmp/case_01.json <<EOF
{
    "eventType": "CASE_CREATED",
    "source": "RESPONDENT_HOME",
    "channel": "RH",
    "contexts": [
        {
            "id": "015c8f60-fa28-4316-b03c-733881747101",
            "caseType": "HH",
            "address.postcode": "EX1 1BB",
            "address.addressType": "HH",
            "address.uprn": "10023122451"
        },
        {
			"caseRef": "bar",
			"id": "#uuid"
	   }
    ]
}
EOF

curl -s --data @/tmp/case_01.json -H "Content-Type: application/json" --user $EVENT_GEN_USER:$EVENT_GEN_PASSWORD $EVENT_GEN_URL/generate | jq
```

####Response
```
{
  "payloads": [
    {
      "id": "015c8f60-fa28-4316-b03c-733881747101",
      "caseRef": "123456",
      "caseType": "HH",
      "survey": "CENSUS",
      "collectionExerciseId": "c6286a36-a04a-4536-9c3d-64db0e97e377",
      "address": {
        "addressLine1": "Nimrod House",
        "addressLine2": "Harbour Street",
        "addressLine3": "Smithfield",
        "townName": "Exeter",
        "postcode": "EX1 1BB",
        "region": "E",
        "latitude": "51.4934",
        "longitude": "0.0098",
        "uprn": "10023122451",
        "arid": "x",
        "addressType": "HH",
        "addressLevel": null,
        "estabType": "E1"
      },
      "contact": {
        "title": "Sir",
        "forename": "Phil",
        "surname": "Whiles",
        "telNo": "07968583119"
      },
      "actionableFrom": null,
      "handDelivery": false,
      "addressInvalid": false,
      "createdDateTime": "2020-06-01T20:17:46.384Z"
    },
    {
      "id": "4a5fdb0e-72de-4fcc-a520-6ad50aef68a4",
      "caseRef": "bar",
      "caseType": "HH",
      "survey": "CENSUS",
      "collectionExerciseId": "c6286a36-a04a-4536-9c3d-64db0e97e377",
      "address": {
        "addressLine1": "Nimrod House",
        "addressLine2": "Harbour Street",
        "addressLine3": "Smithfield",
        "townName": "Exeter",
        "postcode": "EX1 2TDD",
        "region": "E",
        "latitude": "51.4934",
        "longitude": "0.0098",
        "uprn": "123456789",
        "arid": "x",
        "addressType": "HH",
        "addressLevel": null,
        "estabType": "E1"
      },
      "contact": {
        "title": "Sir",
        "forename": "Phil",
        "surname": "Whiles",
        "telNo": "07968583119"
      },
      "actionableFrom": null,
      "handDelivery": false,
      "addressInvalid": false,
      "createdDateTime": "2020-06-01T20:17:46.384Z"
    }
  ]
}
```

### GET /firestore/wait

This endpoint waits for an object to be created in Firestore. If the object is found before the timeout expires then it returns with a 200 status. If the object is not found before a timeout period is reached then it returns a 404 (not found) status.

The endpoint has 3 mandatory query parameters:
  - **collection**, this holds the name of the collection that we expect the object to be created in.
  - **key**, is the primary key for the object that are waiting for.
  - **timeout**, is the maximum time that we are prepared to wait for the object to appear. This supports units of milliseconds(ms) or seconds(s), eg 'timeout=250ms', 'timeout=2s' or 'timeout=2.5s'

To support waiting on objects whose content we expect to be updated the caller can optionally wait based on the candidate objects timestamp or object content. If candidate objects are to be checked by both timestamp and content then both checks must pass for it to be declared as found.

The caller can optionally specify that age of the object:
  - **newerThan**, is a timestamp that the object must have been updated since. Waiting will continue until a candidate object has an update time greater than the this value, or the timeout period is reached. This value is a long containing the number of milliseconds since the epoch.
Note that when Firestore updates an object it does not set the update timestamp of an object if its contents have not changed.

The caller can optionally wait for the object to contain some expected content:
  - **contentCheckPath**, is an optional path to a field whose content we check to decide if
    an object has been updated, eg 'contact.forename' or 'state'. If the target object does not
    contain the field with the expected value then waiting will continue until it does, or the
    timeout is reached.
  - **expectedValue**, is the value than a field must contain if 'contentCheckPath' has been set.

On success, this endpoint returns the Firestore update timestamp for the found object.

Psuedo code for this endpoint is:

```
do {
  Read object from firestore based on 'collection.key'
  if (object not found) continue;

  if (newerThan set  &&  candidate object timestamp < newerThan) {
      # Object exists, but it is not the updated object
      continue
  }

  if (contentCheckPath set  &&  !expectedValue equals actualFieldContent) {
      # Object exists, but target field doesn't contain expected value
      continue
    }
  }

  return 200 status, with objects update time
} while (timeout not reached)

return 404 status

```


Example command line invocation using Httpie (which actually runs as 'http'):

```
# Wait using HTTPie command:
http --auth generator:hitmeup  get "http://localhost:8171/firestore/wait?collection=case&key=f868fcfc-7280-40ea-ab01-b173ac245da3&timeout=500ms"

# Equivalent command using HTTPie query parameters '==' syntax:
http --auth generator:hitmeup get http://localhost:8171/firestore/wait collection==case key==f868fcfc-7280-40ea-ab01-b173ac245da3 timeout==500ms

# And to wait for an object to be updated:
http --auth generator:hitmeup  get "http://localhost:8171/firestore/wait?collection=case&key=f868fcfc-7280-40ea-ab01-173ac245da3&newerThan=1563801758184&path=contact.forename&value=Bill&timeout=500s"
```

#### Object updates and timestamps

The updating of objects can make it tricky for tests to wait for the updating of an object in Firestore. Tests have the option of waiting for the object update based on:
  - the update time.
  - updated object state. Where possible this is potentially more readable and pretty foolproof.

Waiting based on the objects update timestamp can introduce problems based on differences between the timestamp on the Google server and the machine running the test.
It's for this reason that the endpoint returns an objects update time, as it allows this value to be fed back in to spot subsequent updates.

The typical sequence would be:

  - Invoke event generator to feed in event.
  - Wait for object to appear in Firestore. Store its update timestamp.
  - Use event generator to send in updated object.
  - Wait for update to appear in Firestore. The 'newerThan' timestamp value is specified as the captured timestamp of the initial create.


### GET /rabbit/create/{eventType}

This endpoint creates a rabbit queue and binding for the supplied event type. The created queue name is
the same as the routing key. This endpoint returns a String containing the name of the queue.

Rabbit doesn't mind if the queue/binding already exist.

```
http --auth generator:hitmeup GET http://localhost:8171/rabbit/create/SURVEY_LAUNCHED
```

### GET /rabbit/flush/{queueName}

This endpoint purges the contents of the named queue. This allows tests to ensure that outbound queues are in an empty
state before running a test.

It returns the number of messages which were deleted.

```
http --auth generator:hitmeup GET "http://localhost:8171/rabbit/flush/Case.SurveyLaunched"
```


### GET /rabbit/get/{queueName}?timeout={timeoutString}

This endpoint gets the next message from the named queue. If the queue is empty and no message arrives before the expiry of the timeout then it returns with a 404 (Not Found) status.

If a message is found in time then the content of its body is returned.

```
http --auth generator:hitmeup GET "http://localhost:8171/rabbit/get/event.response.authentication?timeout=500ms"
```

### GET /rabbit/get/{queueName}?clazzName={className}&timeout={timeoutString}

This endpoint is a variant of the previous get. It behaves the same as previous get endpoint except that it uses Jackson
to convert the messsage payload to a Java object. In order to return a Json string it then converts the object back into
Json. This endpoint is really only useful for manual testing/debugging of the underlying method in RabbitHelper.java.

```
 http --auth generator:hitmeup GET "http://localhost:8171/rabbit/get/object/event.response.authentication?clazzName=uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent&timeout=500ms"
 ```

### GET /rabbit/send

This endpoint uses the RabbitHelper to send a hardcoded message. Sending messages is the primary use of the EventGenerator so this inflexible endpoint is only useful for manual testing/debugging of RabbitHelper.

```
http --auth generator:hitmeup GET "http://localhost:8171/rabbit/send"
```

###  GET /rabbit/close

This endpoint cleanly closes the Rabbit connection.

```
http --auth generator:hitmeup GET "http://localhost:8171/rabbit/close"
```


## Copyright
Copyright (C) 2019 Crown Copyright (Office for National Statistics)

