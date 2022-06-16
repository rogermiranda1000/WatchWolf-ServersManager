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
		readArray # TODO maps
		readArray # TODO plugins
		
		mc_type=`readString`
		err=$?
		mc_version=`readString`
		err=$(($err | $?))
		
		readArray # TODO config
		
		if [ $err -eq 0 ]; then
			data=`./ServersManager.sh "$mc_type" "$mc_version" "$SOCAT_PEERADDR"` # @return IP & fifo msg & fifo socket
			
			# send IP
			ip=`echo "$data" | cut -d$'\n' -f1 | tr -d '\n'`
			echo -n -e '\x10\x01' # ServersManager response header
			sendString "$ip"
			
			msg_fifo=`echo "$data" | cut -d$'\n' -f2`
			socket_fifo=`echo "$data" | cut -d$'\n' -f3`
			# to not block the read
			exec 3<>"$msg_fifo"
			exec 4<>"$socket_fifo"
			while true; do
				while
						IFS= read -t 0.1 -u 3 -r msg; statusA=$?
						IFS= read -t 0.1 -u 4 -r socket; statusB=$?
						[ $statusA -eq 0 ] || [ $statusB -eq 0 ]; do
					if [ ! -z "$msg" ]; then
						echo "> $msg" >&2 # TODO send errors (remove FD redirect)
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
							echo "-" | awk '{printf "%c%c", 0x10, 0x02}' # server started notification
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
