#!/bin/bash

# default variables
keep=0

# parse params
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --keep) keep=1 ;;
        
        *) echo "[e] Unknown parameter passed: $1" >&2 ; exit 1 ;;
    esac
    shift
done

# some utilities
wsl_mode(){ echo "echo 'Hello world'" | powershell.exe >/dev/null 2>&1; return $?; }
get_ip(){ wsl_mode; if [ $? -eq 0 ]; then echo "(Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias Ethernet).IPAddress" | powershell.exe 2>/dev/null | tail -n2 | head -n1; else hostname -I | awk '{print $1}';fi }

# run
echo "[v] Running..."
export MACHINE_IP=$(get_ip) && export PUBLIC_IP=$(curl ifconfig.me) && export PARENT_PWD=$(pwd) && docker compose up --no-build
# TODO if it crashes the program stops

# ended
if [ $keep -eq 1 ]; then
    # remove docker
    echo "[v] Container done; removing it..."
    docker compose rm --force

    # TODO delete open servers

    # remove tmp folder data
    rm -rf tmp/*
fi