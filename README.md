# WatchWolf - ServersManager
Responsible to start the WatchWolf's Servers. For more information about WatchWolf check [TODO](https://github.com/rogermiranda1000).

The ServersManager is responsable of implementing WatchWolf ServersManager's protocols, and starting the Minecraft Servers whenever it's needed. This concrete implementation will use Docker (to ensure security), and it will be developed using Bash and the TCP port 8000.


## Dependencies

- [Docker](https://www.docker.com/get-started/)
- Ubuntu with Java Docker image: `docker pull openjdk:8`, `docker pull openjdk:16`, `docker pull openjdk:17`
- socat with Docker image: `docker pull ubuntu`
- Link the file with socat (you need to be in the WatchWolf-ServersManager directory):
  `sudo docker run --privileged=true -i --rm --name ServersManager -p 8000:8000 -v /var/run/docker.sock:/var/run/docker.sock -v "$(pwd)":"$(pwd)" ubuntu:latest sh -c "echo '[*] Preparing ServersManager...' ; apt-get -qq update ; DEBIAN_FRONTEND=noninteractive apt-get install -y socat docker.io gawk procmail dos2unix jq unzip >/dev/null ; echo '[*] ServersManager ready.' ; cd $(pwd) ; dos2unix ServersManager.sh ServersManagerConnector.sh SpigotBuilder.sh ; chmod +x ServersManager.sh ServersManagerConnector.sh SpigotBuilder.sh ; rm ServersManager.lock 2>/dev/null ; socat -d -d tcp-l:8000,pktinfo,keepalive,keepidle=10,keepintvl=10,keepcnt=100,ignoreeof,fork system:'bash ./ServersManagerConnector.sh'"`
  
  To launch the manager in debug mode launch `bash -x ./ServersManagerConnector.sh` with socat.