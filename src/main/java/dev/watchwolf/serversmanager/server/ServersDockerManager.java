package dev.watchwolf.serversmanager.server;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.utils.DockerUtilities;
import dev.watchwolf.serversmanager.server.plugins.UnableToAchievePluginException;

import java.io.IOException;
import java.util.Collection;

public class ServersDockerManager {
    public static final String TARGET_SERVER_JAR = "ServersManager.jar";

    private static String getDockerCommand(String ram) {
        String ramParam = "-XX:MaxRAMFraction=1"; // unlimited memory
        if (ram != null) {
            // RAM limit specified
            // TODO `java_ram_param="-Xmx${memory^^}"`
        }

        return "java " + ramParam + " -jar " + TARGET_SERVER_JAR + " nogui"; // run the server
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

    public String startServer(String serverType, String serverVersion, Collection<Plugin> plugins, WorldType worldType, Collection<ConfigFile> maps, Collection<ConfigFile> configFiles) throws IOException,ServerJarUnavailableException {
        String path = ServerRequirements.setupFolder(serverType, serverVersion, plugins, worldType, maps, configFiles, TARGET_SERVER_JAR);
        String serverId = serverType + "_" + serverVersion + "-" + ServerRequirements.getHashFromServerPath(path);

        String dockerCmd = getDockerCommand(null); // TODO specify ram

        System.out.println("Starting " + serverType + " " + serverVersion + " server on " + path + "...");

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        CreateContainerResponse container;
        StartContainerCmd cnt;
        synchronized (ServersDockerManager.class) {
            int port = getNextServerPort();
            int socketPort = port + 1;

            // equivalent to:
            // docker run -i --rm --name "$id" -p "$port:$port/tcp" -p "$port:$port/udp" -p "$socket_port:$socket_port" ${memory:+"--memory=$memory"} ${cpus:+"--cpus=$cpus"} -v "$(pwd)/$path":/server "openjdk:$java_version"
            // TODO specify max memory
            container = dockerClient.createContainerCmd("openjdk:" + DockerUtilities.getJavaVersion(serverVersion))
                    .withName(serverId)
                    .withPortBindings(PortBinding.parse(port + ":" + port + "/tcp"),
                            PortBinding.parse(port + ":" + port + "/udp"),
                            PortBinding.parse(socketPort + ":" + socketPort))
                    //.withVolumes(Volume.parse(path + ":/server"))
                    .withBinds(Bind.parse(path + ":/server"))
                    .withWorkingDir("/server")
                    .withEntrypoint("/bin/sh", "-c")
                    .withCmd(dockerCmd).exec();
            cnt = dockerClient.startContainerCmd(container.getId());
            cnt.exec();
        }

        // TODO on close run:
        // dockerClient.killContainerCmd(container.getId()).exec();
        // dockerClient.removeContainerCmd(container.getId()).exec();
        // and clear the folder

        return ServersDockerManager.getStartedServerIp(cnt.getContainerId(), null); // TODO caller
    }
}
