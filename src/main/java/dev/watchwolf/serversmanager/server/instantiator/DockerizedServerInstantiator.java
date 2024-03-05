package dev.watchwolf.serversmanager.server.instantiator;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import dev.watchwolf.serversmanager.server.ServerRequirements;

import java.nio.file.Path;

public class DockerizedServerInstantiator implements ServerInstantiator {
    private static String getDockerCommand(String jarName, String ram) {
        String ramParam = "-XX:MaxRAMFraction=1"; // unlimited memory
        if (ram != null) {
            // RAM limit specified
            // TODO `java_ram_param="-Xmx${memory^^}"`
        }

        return "java " + ramParam + " -jar " + jarName + " nogui"; // run the server
    }

    /**
     * Gets the max port used by ServersManager instances, and then adds one.
     * @return Next port that ServersManager will have to use
     */
    private static synchronized int getNextServerPort() {
        return 25565;
    }

    private static String getStartedServerIp(String dockerId, String callerIp) {
        return ""; // TODO ask IpManager
    }

    @Override
    public Server startServer(Path folderLocation, String entrypoint, int javaVersion) {
        String serverId = "MC_Server-" + ServerRequirements.getHashFromServerPath(folderLocation.toString());
        String dockerCmd = getDockerCommand(entrypoint, null); // TODO specify ram

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        CreateContainerResponse container;
        StartContainerCmd cnt;
        synchronized (DockerizedServerInstantiator.class) {
            int port = getNextServerPort();
            int socketPort = port + 1;

            // equivalent to:
            // docker run -i --rm --name "$id" -p "$port:$port/tcp" -p "$port:$port/udp" -p "$socket_port:$socket_port" ${memory:+"--memory=$memory"} ${cpus:+"--cpus=$cpus"} -v "$(pwd)/$path":/server "openjdk:$java_version"
            // TODO specify max memory
            container = dockerClient.createContainerCmd("openjdk:" + javaVersion)
                    .withName(serverId)
                    .withPortBindings(PortBinding.parse(port + ":" + port + "/tcp"),
                            PortBinding.parse(port + ":" + port + "/udp"),
                            PortBinding.parse(socketPort + ":" + socketPort))
                    .withBinds(Bind.parse(folderLocation.toString() + ":/server"))
                    .withWorkingDir("/server")
                    .withEntrypoint("/bin/sh", "-c")
                    .withCmd(dockerCmd).exec();
            cnt = dockerClient.startContainerCmd(container.getId());
            cnt.exec();
        }

        Server r = new Server(DockerizedServerInstantiator.getStartedServerIp(cnt.getContainerId(), null)); // TODO caller

        r.subscribeToServerStoppedEvents(() -> {
            // TODO on close run:
            // dockerClient.killContainerCmd(container.getId()).exec();
            // dockerClient.removeContainerCmd(container.getId()).exec();
            // and clear the folder
        });

        // TODO launch server stopped event when stopped

        return r;
    }
}
