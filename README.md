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

This endpoint waits for an object to be created in Firestore. If the object is found before the timeout expires then it returns with a 200 status. If the object is not found before a timeout period is reached then it returns a 404 (not found) status. 

The endpoint has 3 mandatory query parameters:
  - **collection**, this holds the name of the collection that we expect the object to be created in.
  - **key**, is the primary key for the object that are waiting for.
  - **timeout**, is the maximum time that we are prepared to wait for the object to appear. This supports units of milliseconds(ms) or seconds(s), eg 'timeout=250ms', 'timeout=2s' or 'timeout=2.5s'

To support waiting on objects whose content we expect to be updated the caller can optionally wait based on the candidate objects timestamp or object content. If candidate objects are to be checked by both timestamp and content then both checks must pass for it to be declared as found.

The caller can optionally specify that age of the object:
  - **newerThan**, is a timestamp that the object must have been updated since. Waiting will continue until a candidate object has an update time greater than the this value, or the timeout period is reached. This value is a long containing the number of milliseconds since the epoch.
Note that when Firestore updates an object it does not set the update timestamp of an object if it's contents have not changed.   
    
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
http --auth generator:hitmeup  get "http://localhost:8171/firestore/wait?collection=case&key=f868fcfc-7280-40ea-ab01-173ac245da3&newerThan=1563801758184&path=contact.forename&value=Phil&timeout=500s"
```

#### Object updates and timestamps

The updating of objects can make it tricky for tests to wait for the updating of an object in Firestore. Tests have the option of waiting for the object update based on:
  - the update time. 
  - updated object state. Where possible this is potentially more readable and pretty foolproof.

Waiting based on the objects update timestamp can introduce problems based on differences between the timestamp on the Google server and the machine running the test.
It's for this reason that the endpoint returns an objects update time, as it allows this value to be fed back in to spot subsequent updates.

The typical sequence would be:

  - Invoke event generator to feed in event.
  - Wait for object to appear in Firestore. Store it's update timestamp.
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

