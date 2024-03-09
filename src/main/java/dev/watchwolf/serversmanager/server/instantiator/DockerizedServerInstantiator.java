package dev.watchwolf.serversmanager.server.instantiator;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import dev.watchwolf.serversmanager.server.ServerRequirements;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class DockerizedServerInstantiator implements ServerInstantiator {
    public static class StdioAdapter extends ResultCallback.Adapter<Frame> {
        private final String serverId;

        public StdioAdapter(String serverId) {
            this.serverId = serverId;
        }

        @Override
        public void onNext(Frame object) {
            this.onLineGot(new String(object.getPayload()), object.getStreamType().equals(StreamType.STDERR));
        }

        private void onLineGot(String line, boolean stderr) {
            System.out.println("[" + this.serverId + "] " + (stderr ? "(err) " : "") + line);
        }
    }

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
        return 25565; // TODO get port
    }

    private static String getStartedServerIp(String dockerId) {
        return "127.0.0.1:25565"; // TODO get port
    }

    /**
     * Attaches Docker output to a callback method
     * @param dockerClient Docker client
     * @param container Container to attach
     * @param callback Object with the callback method to call
     */
    private static void attachStdio(DockerClient dockerClient, CreateContainerResponse container, ResultCallback<Frame> callback) {
        dockerClient.logContainerCmd(container.getId())
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(callback);
    }

    @Override
    public Server startServer(Path folderLocation, String entrypoint, int javaVersion) {
        String serverId = "MC_Server-" + ServerRequirements.getHashFromServerPath(folderLocation.toString());
        String dockerCmd = getDockerCommand(entrypoint, null); // TODO specify ram

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        final DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

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

        DockerizedServerInstantiator.attachStdio(dockerClient, container, new StdioAdapter(serverId));

        Server r = new Server(DockerizedServerInstantiator.getStartedServerIp(cnt.getContainerId()));

        // we need to perform some cleanup if the server stops
        r.subscribeToServerStoppedEvents(() -> {
            // on server close, close the container
            try {
                dockerClient.killContainerCmd(container.getId()).exec();
            } catch (Exception ignore) {}
            dockerClient.removeContainerCmd(container.getId()).exec();
        });

        // TODO launch server stopped event when stopped

        return r;
    }
}
