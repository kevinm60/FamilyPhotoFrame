#!/bin/bash

if [ ! -e ~/bin/plantuml.jar ]; then
    echo "~/bin/plantuml.jar not found"
    exit 1
fi

for pic in $(ls *.uml); do
    echo "refreshing $pic"
    java -jar ~/bin/plantuml.jar $pic
done
