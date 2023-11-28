#!/bin/bash
echo 'Exporting dependencies to dependencies.txt'

original_file="./dependencies.txt"
temp_file="./temp_dependencies.txt"

./gradlew -q library:dependencies --configuration api | grep -e '^\\---' -e '^+---' > "$temp_file" && ./gradlew -q library:dependencies --configuration implementation | grep -e '^\\---' -e '^+---' >> "$temp_file"

if cmp -s "$original_file" "$temp_file"; then
	echo -e "\e[33mThe list of dependencies wasn't updated.\e[0m"
	rm -f "$temp_file"
else
    #printf "The list of dependencies was updated\n"
	echo -e "\e[34mThe list of dependencies was updated\e[0m"
	mv "$temp_file" "$original_file"
fi

echo -e "\e[32mDone\e[0m"