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

type=$(( $type + (`extract_bits 3 0 $first` << 8) ))

if [ `extract_bits 7 4 $first` -eq 3 ]; then
	# return from Server; it can only be a server started response
	if [ $type -ne 1 ]; then
		exit 2 # unimplemented
	fi
	
	type=`readOneByte`
	err=$?
	if [ $err -ne 0 ]; then
		exit 1
	elif [ $type -ne 0 ]; then
		exit 2 # unimplemented
	fi
	type=`readOneByte`
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
elif [ `extract_bits 7 4 $first` -ne 0 ]; then
	exit 2 # return type set, or destiny not ServersManager
fi

case $type in
	1)
		mc_type=`readString`
		err=$?
		mc_version=`readString`
		err=$(($err | $?))
		
		if [ $err -eq 0 ]; then
			if [ -z "$USE_X" ]; then
				data=`./ServersManager.sh "$mc_type" "$mc_version" "$SOCAT_PEERADDR"` # @return IP & fifo msg & fifo socket
			else
				data=`bash "$USE_X" ./ServersManager.sh "$mc_type" "$mc_version" "$SOCAT_PEERADDR"` # @return IP & fifo msg & fifo socket
			fi
			# ServersManager already reads the rest of the packet
			
			# send IP
			ip=`echo "$data" | cut -d$'\n' -f1 | tr -d '\n'`
			echo -n -e '\x10\x01' # ServersManager response header
			sendString "$ip"
			
			msg_fifo=`echo "$data" | cut -d$'\n' -f2`
			socket_fifo=`echo "$data" | cut -d$'\n' -f3`
			# to not block the read
			exec 3<>"$msg_fifo"
			exec 4<>"$socket_fifo"
			
			error_log=""
			
			while true; do
				while
						IFS= read -t 0.1 -u 3 -r msg; statusA=$?
						IFS= read -t 0.1 -u 4 -r socket; statusB=$?
						[ $statusA -eq 0 ] || [ $statusB -eq 0 ]; do
					if [ ! -z "$msg" ]; then
						type=`echo "$msg" | grep -o -P '(?<=^\[\d{2}:\d{2}:\d{2} )((ERROR)|(INFO))(?=\]: )'`
						if [ ! -z "$error_log" ] || [ "$type" == "ERROR" ]; then
							# error
							if [ ! -z "$error_log" ] && [ ! -z "$type" ]; then
								# new line; send
								echo -e "> $error_log" >&2
								echo -n -e '\x10\x03'; sendString "$error_log" # error notification
								error_log="" # reset
							else
								# append
								error_log="$error_log\n${msg//$'\t'/'\t'}"
							fi
							
							if [ "$type" == "ERROR" ]; then
								# the message was an error; we need to add it to the (already emptied) queue
								error_log="${msg:18}" # remove the timestamp
							fi
						else
							# not an error; just log
							echo "> $msg" >&2
						fi
					fi
					if [ ! -z "$socket" ]; then
						if [ "$socket" == "end" ]; then
							# end of session
							# close FD
							exec 3>&-
							exec 4>&-
							rm -f "$msg_fifo"
							rm -f "$socket_fifo";
							exit 0
						elif [ "$socket" == "started" ]; then
							# server started
							echo -n -e '\x10\x02' # server started notification
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
