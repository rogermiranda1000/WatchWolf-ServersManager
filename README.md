# WatchWolf - ServersManager
Responsible to start the WatchWolf's Servers. For more information about WatchWolf check [TODO](https://github.com/rogermiranda1000).

The ServersManager is responsable of implementing WatchWolf ServersManager's protocols, and starting the Minecraft Servers whenever it's needed. This concrete implementation will use Docker (to ensure security), and it will be developed using Bash and the TCP port 8000.


## Dependencies

- [Docker](https://www.docker.com/get-started/)
- Ubuntu with Java Docker image: `docker pull openjdk:8`, `docker pull openjdk:16`, `docker pull openjdk:17`
- socat with Docker image: `docker pull ubuntu`
- Link the file with socat (you need to be in the WatchWolf-ServersManager directory):
  `wsl_mode(){ echo "echo 'Hello world'" | powershell.exe >/dev/null 2>&1; return $?; } ; get_ip(){ wsl_mode; if [ $? -eq 0 ]; then echo "(Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias Ethernet).IPAddress" | powershell.exe 2>/dev/null | tail -n2 | head -n1; else hostname -I | awk '{print $1}';fi } ; sudo docker run --privileged=true -i --rm --name ServersManager -p 8000:8000 -v /var/run/docker.sock:/var/run/docker.sock -v "$(pwd)":"$(pwd)" --env MACHINE_IP=$(get_ip) --env PUBLIC_IP=$(curl ifconfig.me) --env WSL_MODE=$(wsl_mode ; echo $? | grep -c 0) ubuntu:latest sh -c "echo '[*] Preparing ServersManager...' ; apt-get -qq update ; DEBIAN_FRONTEND=noninteractive apt-get install -y socat docker.io gawk procmail dos2unix jq unzip wget ipcalc >/dev/null ; echo '[*] ServersManager ready.' ; cd $(pwd) ; dos2unix ServersManager.sh ServersManagerConnector.sh SpigotBuilder.sh ; chmod +x ServersManager.sh ServersManagerConnector.sh SpigotBuilder.sh ; rm ServersManager.lock 2>/dev/null ; socat -d -d tcp-l:8000,pktinfo,keepalive,keepidle=10,keepintvl=10,keepcnt=100,ignoreeof,fork system:'bash ./ServersManagerConnector.sh'"`
  
  To launch the manager in debug mode launch `bash -x ./ServersManagerConnector.sh` with socat.
  
  You can add to the Docker part `--env memory=<GB limit of RAM>g` and `--env cpus=<number of logic cores limit>` to limit each server consumption. Check [Docker - Runtime options with CPUs](https://docs.docker.com/config/containers/resource_constraints/#cpu) and [Docker - Runtime options with Memory](https://docs.docker.com/config/containers/resource_constraints/#limit-a-containers-access-to-memory) for reference.
