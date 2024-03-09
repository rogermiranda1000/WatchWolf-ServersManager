package dev.watchwolf.serversmanager;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.rpc.stubs.serversmanager.CapturedExceptionEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServerStartedEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServersManagerPetitions;
import dev.watchwolf.serversmanager.rpc.RequesteeIpGetter;
import dev.watchwolf.serversmanager.rpc.ServersManagerLocalImplementation;
import dev.watchwolf.serversmanager.server.ServersManager;
import dev.watchwolf.serversmanager.server.instantiator.DockerizedServerInstantiator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ITServersManagerShould {
    public static class LocalIpRequesteeMock implements RequesteeIpGetter {
        @Override
        public InetSocketAddress getRequesteeIp() {
            return new InetSocketAddress("127.0.0.1", 45000);
        }
    }

    public static String startServer(ServerStartedEvent onServerStart) throws IOException {
        CapturedExceptionEvent capturedExceptionEventManager = (err) -> {
            System.err.println("[e] Error: " + err);
        };

        RequesteeIpGetter ipGetter = new LocalIpRequesteeMock();

        ServersManager serversManager = new ServersManager(new DockerizedServerInstantiator());
        ServersManagerPetitions serversManagerPetitions = new ServersManagerLocalImplementation(serversManager, onServerStart, capturedExceptionEventManager, ipGetter);
        ArrayList<Plugin> plugins = new ArrayList<>();

        // start the server
        return serversManagerPetitions.startServer("Spigot", "1.19", plugins, WorldType.FLAT, new ArrayList<>(), new ArrayList<>());
    }

    @Test
    public void startAServer() throws Exception {
        final AtomicBoolean started = new AtomicBoolean(false);

        ServerStartedEvent serverStartedEventManager = () -> {
            System.out.println("[!] Server started");
            synchronized (started) {
                if (started.get()) fail("Got two 'server started' events");
                started.set(true);
                started.notify();
            }
        };

        // start the server
        String serverIp = startServer(serverStartedEventManager);
        assertNotEquals("", serverIp, "Expected a server IP; got error instead");

        // wait for server to notify
        int timeout = 15*60_000; // 15 minutes timeout
        synchronized (started) {
            started.wait(timeout);
            assertTrue(started.get(), "Didn't get server started event");
        }
    }
}
