#!/bin/bash

# some utilities
wsl_mode(){ echo "echo 'Hello world'" | powershell.exe >/dev/null 2>&1; return $?; }
get_ip(){ wsl_mode; if [ $? -eq 0 ]; then echo '(Get-NetIPConfiguration |  Where-Object { $_.IPv4DefaultGateway -ne $null -and $_.NetAdapter.Status -ne "Disconnected" }).IPv4Address.IPAddress' | powershell.exe 2>/dev/null | tail -n2 | head -n1 | tr -d "\n" | tr -d "\r"; else hostname -I | awk '{print $1}';fi }

script_path=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# run
echo "[v] Running..."
export MACHINE_IP=$(get_ip) && export PUBLIC_IP=$(curl ifconfig.me) && export PARENT_PWD="$script_path" && export SERVER_PATH_SHIFT="." && docker compose up --no-build --detach
# TODO run `docker compose rm --force` once it finishes