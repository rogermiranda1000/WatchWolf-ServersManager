package dev.watchwolf.serversmanager.rpc;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import dev.watchwolf.core.entities.ServerType;
import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.rpc.stubs.serversmanager.CapturedExceptionEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServerStartedEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServersManagerPetitions;
import dev.watchwolf.core.utils.DockerUtilities;
import dev.watchwolf.serversmanager.server.ServerRequirements;
import dev.watchwolf.serversmanager.server.ServersDockerManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An implementation of ServersManager using Docker to launch the servers.
 */
public class ServersManagerLocalImplementation implements ServersManagerPetitions {
    private final ServerStartedEvent serverStartedEventManager;
    private final CapturedExceptionEvent capturedExceptionEventManager;

    public ServersManagerLocalImplementation(ServerStartedEvent serverStartedEventManager, CapturedExceptionEvent capturedExceptionEventManager) {
        this.serverStartedEventManager = serverStartedEventManager;
        this.capturedExceptionEventManager = capturedExceptionEventManager;
    }

    @Override
    public void nop() throws IOException { }

    @Override
    public String startServer(String serverType, String serverVersion, Collection<Plugin> plugins, WorldType worldType, Collection<ConfigFile> maps, Collection<ConfigFile> configFiles) throws IOException {
        System.out.println("Starting server...");
        return ServersDockerManager.startServer(serverType, serverVersion, plugins, worldType, maps, configFiles);
    }

    public static void main(String[] args) throws IOException {
        ServerStartedEvent serverStartedEventManager = () -> {
            System.out.println("[!] Server started");
        };
        CapturedExceptionEvent capturedExceptionEventManager = (err) -> {
            System.err.println("[e] Error: " + err);
        };

        ServersManagerPetitions serversManagerPetitions = new ServersManagerLocalImplementation(serverStartedEventManager, capturedExceptionEventManager);
        ArrayList<Plugin> plugins = new ArrayList<>();

        serversManagerPetitions.startServer("Spigot", "1.20", plugins, WorldType.FLAT, new ArrayList<>(), new ArrayList<>());
    }
}
