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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ITServersManagerShould {
    @Test
    public void startAServer() throws Exception {
        final AtomicBoolean started = new AtomicBoolean(false);

        ServerStartedEvent serverStartedEventManager = () -> {
            System.out.println("[!] Server started");
            synchronized (started) {
                started.set(true);
                started.notify();
            }
        };

        CapturedExceptionEvent capturedExceptionEventManager = (err) -> {
            System.err.println("[e] Error: " + err);
        };

        ServersManager serversManager = new ServersManager(new DockerizedServerInstantiator());
        ServersManagerPetitions serversManagerPetitions = new ServersManagerLocalImplementation(serversManager, serverStartedEventManager, capturedExceptionEventManager);
        ArrayList<Plugin> plugins = new ArrayList<>();

        // start the server
        String serverIp = serversManagerPetitions.startServer("Spigot", "1.19", plugins, WorldType.FLAT, new ArrayList<>(), new ArrayList<>());
        assertNotEquals("", serverIp, "Expected a server IP; got error instead");

        // wait for server to notify
        int timeout = 120_000;
        synchronized (started) {
            started.wait(timeout);
            assertTrue(started.get(), "Didn't get server started event");
        }
    }
}
