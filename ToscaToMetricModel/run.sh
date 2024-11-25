#!/bin/bash

input=$1
output=$2

if [[ "$input" != /* ]]; then
    input="$(pwd)/$input"
fi

if [[ "$output" != /* ]]; then
    output="$(pwd)/$output"
fi

mvn clean compile

mvn -X exec:java -Dexec.mainClass="ToscaToCamlTranslator" -Dexec.args="$input $output" 