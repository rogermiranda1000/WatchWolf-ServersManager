#!/bin/bash

source ./SpigotBuilder.sh
source ./ConnectorHelper.sh

# @param path_offset
function readFileExpandIfZip {
	file=`readFile "$1"`
	
	directory=`echo "$file" | grep -o -P '^.*(?=/[^/]*$)'`
	extension=`echo "$file" | grep -o -P '(?<=\.)[^/.]*$'`
	
	if [ "$extension" == "zip" ]; then
		USE_X=`case "$-" in *x*) echo "-x" ;; esac`
		if [ -z "$USE_X" ]; then
			unzip "$file" -d "$directory" >/dev/null
		else
			unzip "$file" -d "$directory"  >&2 # same, but log
		fi
		
		rm "$file" # already unzipped; delete
	fi
}

# @param output_dir
# @param server_version
function readPlugin {
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
			echo "Requesting usual plugin $plugin" >&2
			copy_usual_plugin "$1" "$2" "$plugin" "$version"
			err=$?
			if [ $err -ne 0 ]; then
				echo "Error finding $plugin" >&2
			fi
		fi
	elif [ $data -eq 1 ]; then
		# uploaded plugin
		url=`readString`
		if [ $err -ne 0 ]; then
			return 1
		fi
		
		spigot_id=`echo "$url" | grep -o -P '(?<=spigotmc.org/resources/)[^/]+' | grep -o -P '\d+$'`
		if [ -z "$spigot_id" ]; then
			wget -P "$1" "$url" >&2
		else
			# Spigot plugin; get plugin from Spiget website
			spigot_plugin_name=`wget -q -O - "https://api.spiget.org/v2/resources/$spigot_id" | jq -r .name`
			
			spigot_plugin_version=`echo "$url" | grep -o -P '(?<=/download\?version=)[^/]+$'`
			if [ -z "$spigot_plugin_version" ]; then
				url="https://api.spiget.org/v2/resources/$spigot_id/download"
			else
				url="https://api.spiget.org/v2/resources/$spigot_id/versions/$spigot_plugin_version/download"
			fi
			
			wget -O "$1$spigot_plugin_name.jar" "$url" >&2
		fi
	elif [ $data -eq 2 ]; then
		# file plugin
		readFileExpandIfZip "$uuid/plugins/"
	else
		# TODO other plugins
		echo "Uknown plugin type ($data)" >&2
		return 2
	fi
	
	return 0 # all ok
}

# (indirect param) packet containing the plugins, worlds and config files
# @param server_type
# @param server_version
# @param requestee_ip
# @param use_port
# @param reply_ip
# @param server_port
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
	echo -e "online-mode=false\nwhite-list=true\nmotd=Minecraft test server\nmax-players=100\nserver-port=$6\nspawn-protection=0" > "$uuid/server.properties" # non-default server properties
	cp "server-types/$1/$2.jar" "$uuid/server.jar" # server type&version
	
	# copy plugins
	num_plugins=`readShort`
	arr_size=$num_plugins
	err=$?
	if [ $err -ne 0 ]; then
		return 1
	fi
	for (( p=0; p < $num_plugins; p++ )); do
		readPlugin "$uuid/plugins/" "$2"
		if [ $err -ne 0 ]; then
			return $err
		fi
	done
	
	# copy world & config files
	num_files=`readShort`
	err=$?
	if [ $err -ne 0 ]; then
		return 1
	fi
	for (( f=0; f < $num_files; f++ )); do
		readFileExpandIfZip "$uuid/"
	done
	
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
	# 0.0.0.0:8001-8002->8001-8002/tcp
	docker container ls --format "table {{.Ports}}" -a | tail -n +2 | while read ports; do
		line=`echo "$ports" | awk '{ for(i=1;i<=NF;i++) print $i }'`
		# extract the ports from one line
		echo "$line" | grep -o -P '(?<=[:-])\d+(?=-)'
	done
}

# @return Returns the unused Docker port closer to n=0 using 8001+n*2
function get_port {
	port="8001"
	while [ `get_docker_ports | grep -E "$port$" -c` -ne 0 ]; do
		port=$((port+2))
	done
	
	echo "$port"
	echo "! $port" >&2
}

# Syncronized
sync_file="ServersManager.lock"

# Hard limits
memory_limit="4g"
cpu="4"

if [ -z "$1" ] || [ -z "$2" ]; then
	exit 1 # argumens needed
fi

# reply
manager_ip=`hostname -I | sed 's/ //g'`
manager_port=8000 # change this IP to the ServerManager's one
manager_ip="$manager_ip:$manager_port"

# MC configuration
server_type="$1"
mc_version="$2"
request_ip="$3"
lockfile "$sync_file"	# keep the choosed port
port=`get_port`
socket_port=$((port+1))
get_java_version "$mc_version"
java_version="$?"

path=`setup_server "$server_type" "$mc_version" "$request_ip" "$socket_port" "$manager_ip" "$port"`
# @return docker - port - error fifo path - socket fifo path
if [ $? -eq 0 ]; then
	id="${server_type}_${mc_version}-${path}"
	printf "$id\n$port\n"
	
	# error FD
	fd=`mktemp -u`
	mkfifo -m 600 "$fd"
	printf "$fd\n" # send the FD
	
	fd_socket="/tmp/tmp.$path"
	mkfifo -m 600 "$fd_socket"
	printf "$fd_socket\n"
	
	cmd="cp -r /server/* ~/ ; cd ~/ ; java -Xmx${memory_limit^^} -jar server.jar nogui" # copy server base and run it
	{ docker run -i --rm --name "$id" -p "$port:$port/tcp" -p "$port:$port/udp" -p "$socket_port:$socket_port" --memory="$memory_limit" --cpus="$cpu" -v "$(pwd)/$path":/server:ro "openjdk:$java_version" /bin/bash -c "$cmd" >"$fd" 2>&1; rm -rf "$path"; echo "end" > "$fd_socket"; } >/dev/null 2>&1 & disown # start the server and after finishing, close the connections
	
	# we need the container running before releasing the semaphore
	while [ `docker container ls -a | grep -c "$id"` -eq 0 ]; do
		sleep 1 # wait
	done
	
	rm -f "$sync_file" # release semaphore
else
	rm -f "$sync_file" # release semaphore
	echo "Error" >&2
	exit 1
fi