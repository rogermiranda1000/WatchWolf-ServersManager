#!/bin/bash

source ./SpigotBuilder.sh
source ./ConnectorHelper.sh

# (indirect param) packet containing the plugins, worlds and config files
# @param server_type
# @param server_version
# @param requestee_ip
# @param use_port
# @param reply_ip
function setup_server {
	if [ `ls -d server-types/*/ | grep -c -E "^server-types/$1/$"` -eq 0 ]; then
		return 1 # unimplemented server type
	elif [ `ls "server-types/$1" | grep -c -E "^$2.jar$"` -eq 0 ]; then
		return 1 # invalid version/not downloaded
	fi
	
	uuid=`mktemp -u | cut -c 10-` # generate a directory (it will be used too as a fifo)
	
	mkdir "$uuid"
	mkdir "$uuid/plugins"
	echo "eula=true" > "$uuid/eula.txt" # eula
	echo -e "white-list=true\nmotd=Minecraft test server\nmax-players=8" > "$uuid/server.properties" # non-default server properties
	cp "server-types/$1/$2.jar" "$uuid/server.jar" # server type&version
	
	# copy plugins
	arr_size=`readShort`
	err=$?
	if [ $err -ne 0 ]; then
		return 1
	fi
	for (( i=0; i < $arr_size; i++ )); do
		data=`readOneByte`
		err=$?
		if [ $err -ne 0 ]; then
			return 2
		fi
		
		if [ $data -eq 0 ]; then
			# usual plugin
			plugin=`readString`
			err=$?
			version=`readString`
			err=$(($err | $?))
			if [ $err -eq 0 ]; then
				copy_usual_plugin "$uuid/plugins" "$2" "$plugin" "$version"
				err=$?
				if [ $err -ne 0 ]; then
					echo "Error finding $plugin" >&2
				fi
			fi
		else
			# TODO other plugins
			echo "Uknown plugin type ($data)" >&2
			return 2
		fi
	done
	
	# TODO copy worlds
	readArray
	# TODO copy config files
	readArray
	
	# copy WatchWolf-Server plugin & .yml file
	watchwolf_server=`ls usual-plugins | grep '^WatchWolf-' | sort -r | head -1`
	cp "usual-plugins/$watchwolf_server" "$uuid/plugins/$watchwolf_server"
	mkdir "$uuid/plugins/WatchWolf"
	echo -e "target-ip: $3\nuse-port: $4\nreply: $5\nkey: $uuid" > "$uuid/plugins/WatchWolf/config.yml" # this will tell the plugin the TCP port and the IP that should request the commands, and also the IP to reply back to the ServerManager and the key to link the server to the transmission
	
	echo "$uuid" # return the directory path
	return 0 # all ok
}

# @param plugin_name
# @param mc_version
function get_compatible_plugins {
	while IFS= read -r plugin; do
		version=`echo "$plugin" | awk -v version="$2" -F'-' '{ print $3 "\n" substr($4, 0, length($4)-4) "\n" version }' | sort --version-sort | sed -n 2p` # sort min_version, max_version, and current_version and pick the medium value (that should be current_version)
		if [ "$version" == "$2" ]; then
			echo "$plugin" # compatible plugin; return
		fi
	done < <(ls usual-plugins | grep -P "^$1-.*\.jar$")
}

# @param path
# @param mc_version
# @param plugin_name
# @param plugin_version (if desired)
function copy_usual_plugin {
	if [ $# -gt 3 ] && [ ! -z "$4" ]; then
		# version specified
		plugin=`ls usual-plugins | grep -P "^$3-$4-.*\.jar$"` # TODO check if disponible; TODO control injection on regex
		cp "usual-plugins/$plugin" "$1"
		return 0
	fi
	
	# TODO discard uncompatible plugins
	plugin=`get_compatible_plugins "$3" "$2" | sort -t - -k 2,2 --version-sort -r | head -1` # pick the desired plugin; if there's multiple options, pick the one with the higher version
	if [ -z "$plugin" ]; then
		return 1 # compatible version (or plugin) not found
	fi
	
	cp "usual-plugins/$plugin" "$1"
	return 0
}

function get_docker_ports {
	# It generates an output like this:
	# PORTS
	# 0.0.0.0:8000->8000/tcp
	# 0.0.0.0:8001->8001/tcp, 0.0.0.0:8002->8002/tcp
	docker container ls --format "table {{.Ports}}" -a | tail -n +2 | while read ports; do
		echo "$ports" | tail -n +2 | awk '{ for(i=1;i<=NF;i++) print $i }' | grep -o -P '(?<=:)\d+(?=->)' # extract the ports from one line
	done
}

# @return Returns the unused Docker port closer to n=0 using 8001+n*2
function get_port {
	port="8001"
	while [ `get_docker_ports | grep -E ":$port$" -c` -eq 1 ]; do
		port=$((port+2))
	done
	
	echo "$port"
}

# launch auto-updater
#getAllVersions |
#while read version; do
#	buildVersion `pwd`/server-types/Spigot "$version" >/dev/null 2>&1 &
#done

# Hard limits
memory_limit="4g"
cpu="4"

if [ -z "$1" ] || [ -z "$2" ]; then
	exit 1 # argumens needed
fi

# reply
manager_ip=`hostname -I | sed 's/ //g'` # change this IP to the ServerManager's one
manager_port=8000 # change this IP to the ServerManager's one
manager_ip=`echo "$manager_ip:$manager_port"`

# MC configuration
server_type="$1"
mc_version="$2"
request_ip="$3"
port=`get_port`
socket_port=$((port+1))
get_java_version "$mc_version"
java_version="$?"

path=`setup_server "$server_type" "$mc_version" "$request_ip" "$socket_port" "$manager_ip"`
# @return IP:port - error fifo path - socket fifo path
if [ $? -eq 0 ]; then
	# send IP
	ip="127.0.0.1" # we're using docker; if not we should run `hostname -I | sed 's/ //g'`
	echo "$ip:$port" # print the trimmed ip and port
	
	# error FD
	fd=`mktemp -u`
	mkfifo -m 600 "$fd"
	echo "$fd" # send the FD
	
	fd_socket="/tmp/tmp.$path"
	mkfifo -m 600 "$fd_socket"
	echo "$fd_socket"
	
	cmd="cp -r /server/* ~/ ; cd ~/ ; java -Xmx${memory_limit^^} -jar server.jar nogui" # copy server base and run it
	{ docker run -i --rm --entrypoint /bin/sh --name "${server_type}_${mc_version}-${path}" -p "$port:$port" -p "$socket_port:$socket_port" --network container:ServersManager --memory="$memory_limit" --cpus="$cpu" -v "$(pwd)/$path":/server:ro "openjdk:$java_version" <<< "$cmd" >"$fd" 2>&1; rm -rf "$path"; echo "end" > "$fd_socket"; } >/dev/null & disown # start the server on docker, but remove non-error messages; then remove it
else
	echo "Error"
	exit 1
fi