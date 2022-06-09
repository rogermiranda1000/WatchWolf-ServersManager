#!/bin/bash

# Hard limits
memory_limit="4g"
cpu="4"

# MC configuration
mc_version="1.12.2"
port="8001"
java="8" # TODO change java version with mc_version
cmd="cp -r /server/* ~/ ; cd ~/ ; java -Xmx${memory_limit^^} -jar spigot-$mc_version.jar nogui" # copy server base and run it

sudo docker run -i --rm --name "Minecraft_$mc_version" -p "$port:25565" --memory="$memory_limit" --cpus="$cpu" -v "$(pwd)/server":/server:ro "openjdk:$java" <<< "$cmd"
