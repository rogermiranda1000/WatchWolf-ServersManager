# WatchWolf - ServersManager
Responsible to start the WatchWolf's Servers. For more information about WatchWolf check [TODO](https://github.com/rogermiranda1000).

The ServersManager is responsable of implementing WatchWolf ServersManager's protocols, and starting the Minecraft Servers whenever it's needed. This concrete implementation will use Docker (to ensure security), and it will be developed using Bash.


## Dependencies

- [Docker](https://www.docker.com/get-started/)
- Ubuntu with Java Docker image: `docker pull openjdk:8`, `docker pull openjdk:16`, `docker pull openjdk:17`
- socat: `sudo apt install socat`
- Link the file with socat (`socat -u tcp-l:8000,pktinfo,fork exec:<ServersManagerConnector path>`) at startup (using services or rc.local)


## WSL Firewall

- According to [this issue](https://github.com/microsoft/WSL/issues/4585#issuecomment-610061194), you should un in your Windows powershell this command: `New-NetFirewallRule -DisplayName "WSL" -Direction Inbound  -InterfaceAlias "vEthernet (WSL)"  -Action Allow`.