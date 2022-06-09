#!/bin/bash

memory_limit="7g" # 6GB of RAM for the MC server, 1GB for Ubuntu
cpu="4"

java="8"
mc_version="1.12.2"
cmd="cd /server ; java -Xmx${memory_limit^^} -jar spigot-$mc_version.jar nogui"
sudo docker run -i --rm --entrypoint /bin/sh --name test -p 8080:80 --memory="$memory_limit" --cpus="$cpu" -v "$(pwd)/server":/server "openjdk:$java" <<< "$cmd"
