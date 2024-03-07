package dev.watchwolf.serversmanager.rpc;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.rpc.stubs.serversmanager.CapturedExceptionEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServerStartedEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServersManagerPetitions;
import dev.watchwolf.serversmanager.server.ServerJarUnavailableException;
import dev.watchwolf.serversmanager.server.ServersManager;
import dev.watchwolf.serversmanager.server.instantiator.DockerizedServerInstantiator;
import dev.watchwolf.serversmanager.server.instantiator.Server;
import dev.watchwolf.serversmanager.server.instantiator.ThrowableServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An implementation of ServersManager using Docker to launch the servers.
 */
public class ServersManagerLocalImplementation implements ServersManagerPetitions {
    private final ServersManager serversManager;
    private final ServerStartedEvent serverStartedEventManager;
    private final CapturedExceptionEvent capturedExceptionEventManager;

    public ServersManagerLocalImplementation(ServersManager serversManager, ServerStartedEvent serverStartedEventManager, CapturedExceptionEvent capturedExceptionEventManager) {
        this.serversManager = serversManager;
        this.serverStartedEventManager = serverStartedEventManager;
        this.capturedExceptionEventManager = capturedExceptionEventManager;
    }

    @Override
    public void nop() throws IOException { }

    @Override
    public String startServer(final String serverType, final String serverVersion, Collection<Plugin> plugins, WorldType worldType, Collection<ConfigFile> maps, Collection<ConfigFile> configFiles) throws IOException {
        System.out.println("Starting server...");
        try {
            final ThrowableServer server = this.serversManager.startServer(serverType, serverVersion, plugins, worldType, maps, configFiles);

            server.subscribeToServerStartedEvents(this.serverStartedEventManager);
            server.subscribeToServerStoppedEvents(() -> {
                System.out.println("Server " + serverType + " " + serverVersion + " (" + server.getIp() + ") stopped");
            });
            server.subscribeToExceptionEvents((msg) -> {
                System.err.println("Got exception on " + serverType + " " + serverVersion + " server:\n" + msg);
                this.capturedExceptionEventManager.capturedException(msg);
            });

            return server.getIp();
        } catch (ServerJarUnavailableException ex) {
            String errorMessage = "Couldn't start a " + serverType + " server, on " + serverVersion + ": " + ex.toString();
            System.err.println(errorMessage);
            capturedExceptionEventManager.capturedException(errorMessage);
            return ""; // no server
        }
    }

    public static void main(String[] args) throws IOException {
        // TODO must redirect main; execute RPC setup
        ServerStartedEvent serverStartedEventManager = () -> {
            System.out.println("[!] Server started");
        };
        CapturedExceptionEvent capturedExceptionEventManager = (err) -> {
            System.err.println("[e] Error: " + err);
        };

        ServersManager serversManager = new ServersManager(new DockerizedServerInstantiator());
        ServersManagerPetitions serversManagerPetitions = new ServersManagerLocalImplementation(serversManager, serverStartedEventManager, capturedExceptionEventManager);
        ArrayList<Plugin> plugins = new ArrayList<>();

        serversManagerPetitions.startServer("Spigot", "1.19", plugins, WorldType.FLAT, new ArrayList<>(), new ArrayList<>());
    }
}
