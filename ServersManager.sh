#!/bin/bash

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
	# TODO copy worlds
	# TODO copy config files
	
	# TODO copy plugins
	
	# copy WatchWolf-Server plugin & .yml file
	watchwolf_server=`ls usual-plugins | grep '^WatchWolf-' | sort -r | head -1`
	cp "usual-plugins/$watchwolf_server" "$uuid/plugins/$watchwolf_server"
	mkdir "$uuid/plugins/WatchWolf"
	echo -e "target-ip: $3\nuse-port: $4\nreply: $5\nkey: $uuid" > "$uuid/plugins/WatchWolf/config.yml" # this will tell the plugin the TCP port and the IP that should request the commands, and also the IP to reply back to the ServerManager and the key to link the server to the transmission
	
	# TODO send communication port
	
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
port="8001" # TODO change
get_java_version "$mc_version"
java_version="$?"

path=`setup_server "$server_type" "$mc_version" "$request_ip" $(($port + 1)) "$manager_ip"`
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
	{ sudo docker run -i --rm --entrypoint /bin/sh --name "${server_type}_${mc_version}" -p "$port:25565" -p "$((port+1)):$((port+1))" -p "$manager_port:$manager_port" --memory="$memory_limit" --cpus="$cpu" -v "$(pwd)/$path":/server:ro "openjdk:$java_version" <<< "$cmd" >"$fd"; rm -rf "$path"; echo "end" > "$fd_socket"; } >/dev/null & disown # start the server on docker, but remove non-error messages; then remove it
	# TODO removing the fifo here will stuck the Connector loop?
else
	echo "Error"
	exit 1
fi