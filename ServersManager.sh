#!/bin/bash

# @param server_type
# @param server_version
function setup_server {
	if [ `ls -d server-types/*/ | grep -c -E "^server-types/$1/$"` -eq 0 ]; then
		return 1 # unimplemented server type
	elif [ `ls "server-types/$1" | grep -c -E "^$2.jar$"` -eq 0 ]; then
		return 1 # invalid version/not downloaded
	fi
	
	uuid=`uuidgen` # generate a directory
	
	mkdir "$uuid"
	mkdir "$uuid/plugins"
	echo "eula=true" > "$uuid/eula.txt" # eula
	echo -e "white-list=true\nmotd=Minecraft test server\nmax-players=8" > "$uuid/server.properties" # non-default server properties
	cp "server-types/$1/$2.jar" "$uuid/server.jar" # server type&version
	
	echo "$uuid" # return the directory path
	return 0 # all ok
}

# Hard limits
memory_limit="4g"
cpu="4"

# MC configuration
mc_version="1.12.2"
server_type="Spigot"
port="8001"
java="8" # TODO change java version with mc_version

path=`setup_server "$server_type" "$mc_version"`
if [ $? -eq 0 ]; then
	cmd="cp -r /server/* ~/ ; cd ~/ ; java -Xmx${memory_limit^^} -jar server.jar nogui" # copy server base and run it
	sudo docker run -i --rm --name "${server_type}_${mc_version}" -p "$port:25565" --memory="$memory_limit" --cpus="$cpu" -v "$(pwd)/$path":/server:ro "openjdk:$java" <<< "$cmd"
	rm -rf "$path" # remove the server once finished
else
	echo "Error"
fi