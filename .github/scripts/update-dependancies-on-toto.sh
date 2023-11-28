#!/bin/bash

set -euo pipefail

version=$1
TOTO_API_TOKEN=$2

url='https://admin.appboosty.com'

payload=""

temp_file="./temp_deps.txt"


./gradlew -q library:dependencies --configuration api | grep -e '^\\---' -e '^+---' > "$temp_file" && ./gradlew -q library:dependencies --configuration implementation | grep -e '^\\---' -e '^+---' >> "$temp_file"

input=$temp_file


while IFS= read -r line
do
    readarray -d " "  -t strarr <<<"$line" 

    packagePrefix=$(echo ${strarr[1]} | cut -d':' -f 1)
    packageSuffix=$(echo ${strarr[1]} | cut -d':' -f 2)
    package="$packagePrefix":"$packageSuffix"
    packageVersion=$(echo ${strarr[1]} | cut -d':' -f 3)
    payload+='{"dependacy" : "'"$package"'","version": "'"$packageVersion"'"},'

done < "$input"

rm $temp_file
payload=${payload::-1}

data='{ "ph_version": "'
data+="$version"
data+='", "dependacies": ['
data+=$payload
data+=' ]}'

response=$(
  curl  -s -w "%{http_code}" \
  -X POST \
  -H "Authorization: Bearer $TOTO_API_TOKEN" \
  "$url/v1/phdependacies/phdependacy:create" \
  -d "$data"
  
)

http_code=${response: -3}

 
if [[ $http_code != "200" ]]; then
  echo $response
  exit 1
fi
