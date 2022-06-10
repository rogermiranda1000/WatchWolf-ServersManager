#!/bin/bash

host="localhost"
port="8000"

while read a; do
	echo "($SOCAT_PEERADDR:$SOCAT_PEERPORT) $a"
done

#./ServersManager.sh "Spigot" "1.17.1"