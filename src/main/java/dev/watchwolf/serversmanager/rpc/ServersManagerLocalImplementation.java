package dev.watchwolf.serversmanager.rpc;

import dev.watchwolf.core.entities.ServerType;
import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.rpc.stubs.serversmanager.CapturedExceptionEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServerStartedEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServersManagerPetitions;

import java.io.IOException;
import java.util.Collection;

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
        // TODO run server
        return "";
    }
}
