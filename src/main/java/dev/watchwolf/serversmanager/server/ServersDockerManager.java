package dev.watchwolf.serversmanager.server;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.utils.DockerUtilities;

import java.io.IOException;
import java.util.Collection;

public class ServersDockerManager {
    private static String getDockerCommand(String ram) {
        String ramParam = "-XX:MaxRAMFraction=1"; // unlimited memory
        if (ram != null) {
            // RAM limit specified
            // TODO `java_ram_param="-Xmx${memory^^}"`
        }

        return "java " + ramParam + " -jar server.jar nogui"; // run the server TODO is `cd /server` needed if I changed the working directory?
    }

    private static synchronized int getMaxServerPort() {
        return 25565;
    }

    public static synchronized String startServer(String serverType, String serverVersion, Collection<Plugin> plugins, WorldType worldType, Collection<ConfigFile> maps, Collection<ConfigFile> configFiles) throws IOException {
        String path = ServerRequirements.setupFolder(serverType, serverVersion, plugins, worldType, maps, configFiles);
        int port = getMaxServerPort();
        int socketPort = port+1;
        String serverId = serverType + "_" + serverVersion + "-" + path;

        String dockerCmd = getDockerCommand(null); // TODO specify ram

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        // equivalent to:
        // docker run -i --rm --name "$id" -p "$port:$port/tcp" -p "$port:$port/udp" -p "$socket_port:$socket_port" ${memory:+"--memory=$memory"} ${cpus:+"--cpus=$cpus"} -v "$(pwd)/$path":/server "openjdk:$java_version"
        // TODO specify max memory
        CreateContainerResponse container = dockerClient.createContainerCmd("openjdk:" + DockerUtilities.getJavaVersion(serverVersion))
                .withCmd("/bin/bash -c \"" + dockerCmd + "\"")
                .withName(serverId)
                .withPortBindings(PortBinding.parse(port + ":" + port + "/tcp"))
                .withPortBindings(PortBinding.parse(port + ":" + port + "/udp"))
                .withPortBindings(PortBinding.parse(socketPort + ":" + socketPort))
                .withBinds(Bind.parse(path + ":/server"))
                .withWorkingDir("/server").exec();
        dockerClient.startContainerCmd(container.getId()).exec();

        // TODO on close run:
        // dockerClient.killContainerCmd(container.getId()).exec();
        // and clear the folder

        return ""; // TODO send IP
    }
}
