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

# @param server_version
function get_java_version {
	case `echo "$1" | grep -o -P '^\d+\.\d+'` in # get the first 2 numbers
		"1.18" )
			return 17
			;;
		"1.17" )
			return 16
			;;
		* ) # previous to 1.17
			return 8
			;;
	esac
}

# Hard limits
memory_limit="4g"
cpu="4"

# MC configuration
mc_version="1.17.1" #"1.12.2"
server_type="Spigot"
port="8001"
get_java_version "$mc_version"
java="$?"

path=`setup_server "$server_type" "$mc_version"`
if [ $? -eq 0 ]; then
	cmd="cp -r /server/* ~/ ; cd ~/ ; java -Xmx${memory_limit^^} -jar server.jar nogui" # copy server base and run it
	sudo docker run -i --rm --entrypoint /bin/sh --name "${server_type}_${mc_version}" -p "$port:25565" --memory="$memory_limit" --cpus="$cpu" -v "$(pwd)/$path":/server:ro "openjdk:$java" <<< "$cmd"
	rm -rf "$path" # remove the server once finished
else
	echo "Error"
fi