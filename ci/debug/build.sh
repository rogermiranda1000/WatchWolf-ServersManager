#!/bin/bash

# check for file dependencies
files=`ls .`

if [ `echo "$files" | grep -c -P 'watchwolf-servers-manager-[\d\.]+\.jar'` -ne 1 ]; then
    echo "[e] Make sure to have the WW-ServersManager .jar in the current directory (and only one instance)"
    exit 1
fi

if [ `echo "$files" | grep -c -P 'watchwolf-server-[\d\.]+\.jar'` -ne 1 ]; then
    echo "[e] Make sure to have the WW-Server .jar in the current directory (and only one instance)"
    exit 1
fi

# create dependent folders
mkdir server-types 2>/dev/null
mkdir usual-plugins 2>/dev/null

if [ `ls -l server-types 2>&1 | grep -c '^d'` -eq 0 ]; then
    echo "[w] You don't have any server type in the folder. To get the default server types check the following link:"
    echo "https://github.com/watch-wolf/WatchWolf/blob/main/WatchWolfSetup.sh"
fi
# all checks done; run

# copy WW-Server as a usual plugin
echo "[v] Moving WW-Server to the 'usual plugins' folder..."
version=`echo "$files" | grep -o -P '(?<=watchwolf-server-)[\d\.]+(?=\.jar)'`
cp "watchwolf-server-$version.jar" "usual-plugins/WatchWolf-$version-1.8-LATEST.jar"

# some utilities
wsl_mode(){ echo "echo 'Hello world'" | powershell.exe >/dev/null 2>&1; return $?; }

# build the docker
echo "[v] Building Docker container..."
docker compose build --no-cache --build-arg WSL_MODE=$(wsl_mode ; echo $? | grep -c 0)