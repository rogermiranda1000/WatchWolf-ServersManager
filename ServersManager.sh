#!/bin/bash

memory_limit="7g" # 6GB of RAM for the MC server, 1GB for Ubuntu
cpu="4"

java="8"
cmd="apt-get update && apt-get -y install -qq openjdk-$java-jdk"
sudo docker run -i --rm --entrypoint /bin/sh --name test -p 8080:80 --memory="$memory_limit" --cpus="$cpu" -v "$(pwd)/test":/data ubuntu <<< "$cmd"
