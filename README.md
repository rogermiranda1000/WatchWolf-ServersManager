# WatchWolf - ServersManager
Responsible to start the WatchWolf's Servers. For more information about WatchWolf check [TODO](https://github.com/rogermiranda1000).

The ServersManager is responsable of implementing WatchWolf ServersManager's protocols, and starting the Minecraft Servers whenever it's needed. This concrete implementation will use Docker (to ensure security), and it will be developed using Java.

## Dependencies

- [Docker](https://www.docker.com/get-started/)
- Ubuntu Docker image: `docker pull ubuntu`