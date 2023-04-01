#!/bin/bash

source ./ConnectorHelper.sh

# @param MSB bit number
# @param LSB bit number
# @param data
# @author https://stackoverflow.com/a/15185406/9178470
function extract_bits {
	msb=$1 ; lsb=$2 ; num=$3
	len=$(( $msb + 1 - $lsb ))
	mask=$(( 2 ** $len - 1 ))
	echo $(( ( $num & ( $mask << $lsb ) ) >> $lsb ))
}

# @param client IP
# @param container IP
# @env MACHINE_IP The machine IP
# @env PUBLIC_IP The machine public IP
# @env WSL_MODE Using WSL (1) or native Linux (0)
function get_ip {
	if [ $WSL_MODE -eq 1 ]; then
		echo "$2" # WSL doesn't support external connections; assume it's being called locally
	else
		if [ "$1" != "127.0.0.1" ]; then
			is_private=`echo "$1" | grep -P -c '^(10\.|172\.1[6-9]\.|172\.2[0-9]\.|172\.3[0-1]\.|192\.168\.)'`
			if [ $is_private -eq 1 ]; then
				echo "$MACHINE_IP" # external private connections; send the machine IP
			else
				echo "$PUBLIC_IP" # external public connection; send public IP
			fi
		else
			echo "$2" # it's being called locally; provide docker IP
		fi
	fi
}

USE_X=`case "$-" in *x*) echo "-x" ;; esac`

first=`readOneByte`
err=$?
if [ $err -ne 0 ]; then
	exit 1
fi

type=`readOneByte`
err=$?
if [ $err -ne 0 ]; then
	exit 1
fi

type=$(( ($type << 4) + ($first >> 4) ))
dst_and_return=`extract_bits 3 0 $first`

if [ $dst_and_return -eq 9 ]; then # 1 (return bit) & 001 (from server)
	# return from Server; it can only be a server started response
	if [ $type -ne 1 ]; then
		exit 2 # unimplemented
	fi
	
	type=`readShort`
	if [ $err -ne 0 ]; then
		exit 1
	elif [ $type -ne 2 ]; then
		exit 2 # unimplemented
	fi
	
	# server started -> redirect to the tester
	token=`readString`
	if [ `echo "$token" | grep -c -P '^(\w|-)+$'` -ne 1 ]; then
		exit 1 # not a token
	fi
	
	echo "started" > "/tmp/tmp.${token}"
	
	exit 0
elif [ $dst_and_return -ne 0 ]; then
	exit 2 # return type set, or destiny not ServersManager
fi

case $type in
	1)
		mc_type=`readString`
		err=$?
		mc_version=`readString`
		err=$(($err | $?))
		# ServersManager will read the rest of the packet
		
		if [ $err -eq 0 ]; then
			if [ -z "$USE_X" ]; then
				data=`./ServersManager.sh "$mc_type" "$mc_version" "$SOCAT_PEERADDR"` # @return IP & fifo msg & fifo socket
			else
				data=`bash "$USE_X" ./ServersManager.sh "$mc_type" "$mc_version" "$SOCAT_PEERADDR"` # @return IP & fifo msg & fifo socket
			fi
			# ServersManager already reads the rest of the packet
			

			if [ $? -ne 0 ]; then
				exit 1 # error while starting the server
			fi

			docker_container=`echo "$data" | cut -d$'\n' -f1`
			port=`echo "$data" | cut -d$'\n' -f2`
			msg_fifo=`echo "$data" | cut -d$'\n' -f3`
			socket_fifo=`echo "$data" | cut -d$'\n' -f4`
			
			# to not block the read
			exec 3<>"$msg_fifo"
			exec 4<>"$socket_fifo"
			
			ip="null"
			error_log=""
			
			while true; do
				while
						IFS= read -t 0.02 -u 3 -r msg; statusA=$?
						IFS= read -t 0.01 -u 4 -r socket; statusB=$?
						[ $statusA -eq 0 ] || [ $statusB -eq 0 ]; do
					if [ "$ip" == "null" ] || [ -z "$ip" ]; then
						# docker started; get IP & send it to the Tester
						ip=`docker inspect "$docker_container" 2>/dev/null | jq -r '.[0].NetworkSettings.IPAddress'`
						if [ "$ip" != "null" ] && [ ! -z "$ip" ]; then
							ip=`get_ip "$SOCAT_PEERADDR" "$ip"`
							ip=`echo "$ip:$port" | tr -d '\n'`
							echo "Using MC server's IP $ip" >&2
							
							# send IP
							echo -n -e '\x18\x00' # ServersManager start server response header
							sendString "$ip"
						fi
					fi
					
					if [ ! -z "$msg" ]; then
						type=`echo "$msg" | grep -o -P '(?<=^\[\d{2}:\d{2}:\d{2} )((ERROR)|(INFO))(?=\]: )'` # TODO
						if [ ! -z "$error_log" ] || [ "$type" == "ERROR" ]; then
							# error
							if [ ! -z "$error_log" ] && [ ! -z "$type" ]; then
								# new line; send
								echo -e "> $error_log" >&2
								echo -n -e '\x38\x00'; sendString "$error_log" # error notification
								error_log="" # reset
							else
								# append
								error_log="$error_log\n${msg//$'\t'/'\t'}"
							fi
							
							if [ "$type" == "ERROR" ]; then
								# the message was an error; we need to add it to the (already emptied) queue
								error_log="${msg:18}" # remove the timestamp
							fi
						fi
						if [ -z "$error_log" ] && [ "$type" != "ERROR" ]; then
							# not an error; just log
							if [ -z "$USE_X" ]; then
								echo "> $msg" >&2
							fi
						fi
					fi
					if [ ! -z "$socket" ]; then
						if [ "$socket" == "end" ]; then
							echo "End of session" >&2
							
							# close FD
							exec 3>&-
							exec 4>&-
							rm -f "$msg_fifo"
							rm -f "$socket_fifo";
							
							exit 0
						elif [ "$socket" == "started" ]; then
							echo "Server started" >&2
							echo -n -e '\x28\x00' # server started notification
						else
							echo "Uknown request from socket fifo: $socket" >&2
						fi
					fi
				done
				sleep 1
			done
		else
			echo "Received Start server request, but arguments were invalid" >&2
		fi
		;;
		
	*)
		echo "Uknown request ($type)" >&2
		;;
esac
