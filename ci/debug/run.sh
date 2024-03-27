#!/bin/bash

# some utilities
wsl_mode(){ echo "echo 'Hello world'" | powershell.exe >/dev/null 2>&1; return $?; }
get_ip(){ wsl_mode; if [ $? -eq 0 ]; then echo "(Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias Ethernet).IPAddress" | powershell.exe 2>/dev/null | tail -n2 | head -n1; else hostname -I | awk '{print $1}';fi }

script_path=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
base_path=$(dirname $(dirname "$script_path"))

# run
echo "[v] Running..."
export MACHINE_IP=$(get_ip) && export PUBLIC_IP=$(curl ifconfig.me) && export PARENT_PWD="$base_path" && export SERVER_PATH_SHIFT="./ci/debug" && docker compose up --no-build --detach
# TODO run `docker compose rm --force` once it finishes