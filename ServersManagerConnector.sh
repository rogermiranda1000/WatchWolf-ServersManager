#!/bin/bash

function readOneByte {
	read -r -d '' -N 1 data # don't use scape characters, don't wait for \n to end , and read 1 byte (without waiting if none)
	err="$?"
	if [ "$err" -ne 0 ]; then
		return "$err"
	fi
	
	echo "$data"
	return 0
}

first=`readOneByte`
err="$?"
echo "($SOCAT_PEERADDR:$SOCAT_PEERPORT) $err > $first"

#./ServersManager.sh "Spigot" "1.17.1"