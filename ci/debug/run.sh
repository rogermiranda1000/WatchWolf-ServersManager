#!/bin/bash

# some utilities
wsl_mode(){ echo "echo 'Hello world'" | powershell.exe >/dev/null 2>&1; return $?; }
get_ip(){ wsl_mode; if [ $? -eq 0 ]; then echo "(Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias Ethernet).IPAddress" | powershell.exe 2>/dev/null | tail -n2 | head -n1; else hostname -I | awk '{print $1}';fi }

# run
echo "[v] Running..."
export MACHINE_IP=$(get_ip) && export PUBLIC_IP=$(curl ifconfig.me) && docker compose up --no-build

# remove docker
echo "[v] Container done; removing it..."
docker compose rm --force