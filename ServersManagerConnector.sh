#!/bin/bash

function readOneByte {
	data=`od -N1 -An -vtu1`
	if [ -z "$data" ]; then
		return 1 # not enought data
	fi
	
	echo -n "$data"
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
if [ $err -ne 0 ]; then
	exit 1
fi

if [ `extract_bits 7 4 $first` -ne 0 ]; then
	exit 2 # return type set, or destiny not ServersManager
fi

type=`readOneByte`
err=$?
if [ $err -ne 0 ]; then
	exit 1
fi

type=$(( $type + (`extract_bits 3 0 $first` << 8) ))
echo "($SOCAT_PEERADDR:$SOCAT_PEERPORT) $type"

#./ServersManager.sh "Spigot" "1.17.1"