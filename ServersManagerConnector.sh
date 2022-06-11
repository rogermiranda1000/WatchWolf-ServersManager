#!/bin/bash

function readOneByte {
	read -r -d '' -N 1 data	# don't use scape characters, don't wait for \n to end , and read 1 byte (without waiting if none)
	err=$?
	if [ "$err" -ne 0 ]; then
		return "$err" # error (not enought data?)
	fi
	
	echo -n "$data" | hd | awk '{ print strtonum("0x"$2) }' # read data as "integer"
	return 0
}

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
extract_bits 7 4 $first
extract_bits 3 0 $first
echo "($SOCAT_PEERADDR:$SOCAT_PEERPORT) $err > $first"

#./ServersManager.sh "Spigot" "1.17.1"