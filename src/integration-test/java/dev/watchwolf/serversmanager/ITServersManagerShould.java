package dev.watchwolf.serversmanager;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.rpc.stubs.serversmanager.CapturedExceptionEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServerStartedEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServersManagerPetitions;
import dev.watchwolf.serversmanager.rpc.ServersManagerLocalImplementation;
import dev.watchwolf.serversmanager.server.ServersManager;
import dev.watchwolf.serversmanager.server.instantiator.DockerizedServerInstantiator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class ITServersManagerShould {
    @Test
    public void startAServer() throws Exception {
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
        Thread.sleep(15_000);
    }
}
