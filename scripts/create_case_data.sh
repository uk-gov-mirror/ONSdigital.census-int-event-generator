#!/bin/bash
#
# This script generates a test data file containing case data. 
#
# The generated data file can then be fed into the EventGenerator with a command such as: 
#   curl --data @/tmp/case.json -H "Content-Type: application/json" --user generator:hitmeup  http://localhost:8171/generate | jq
#

if [[ $# -ne 1 ]]; then
    printf 'Usage: create_case_data.sh <number-cases>'
    exit 1
fi

number_cases=$1
base_arid=87711
base_uprn=4000
base_telephone_number="383285"

printf '{\n'
printf '  "eventType": "CASE_UPDATED",\n'
printf '  "source": "SAMPLE_LOADER",\n'
printf '  "channel": "RM",\n'
printf '  "contexts": [\n'

for ((i = 1; i <= $number_cases; i++ )); do
  printf '    {\n'
  printf '      "caseRef": "%d",\n' $i
  printf '      "address.arid": "%d",\n' $(($base_arid+i))
  printf '      "collectionExerciseId": "c6286a36-a04a-4536-9c3d-%012d",\n' $i
  printf '      "contact.telNo": "%d",\n' $(($base_telephone_number+i))
  printf '      "id": "d603817e-bd49-4d21-80f6-%012d",\n' $i
  printf '      "address.uprn": "%d"\n' $(($base_uprn+i))

  if [[ i -lt $number_cases ]]; then
    printf '    },\n'
  else
    printf '    }\n'
  fi
done

printf '  ]\n'
printf '}\n'
