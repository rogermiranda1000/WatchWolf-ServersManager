package dev.watchwolf.serversmanager.server;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.utils.DockerUtilities;
import dev.watchwolf.serversmanager.server.instantiator.ServerInstantiator;
import dev.watchwolf.serversmanager.server.instantiator.ThrowableServer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;

public class ServersManager {
    public static final String TARGET_SERVER_JAR = "ServersManager.jar";

    private final ServerInstantiator serverInstantiator;

    public ServersManager(ServerInstantiator serverInstantiator) {
        this.serverInstantiator = serverInstantiator;
    }

    public ThrowableServer startServer(String serverType, String serverVersion, Collection<Plugin> plugins, WorldType worldType, Collection<ConfigFile> maps, Collection<ConfigFile> configFiles) throws IOException,ServerJarUnavailableException {
        String path = ServerRequirements.setupFolder(serverType, serverVersion, plugins, worldType, maps, configFiles, TARGET_SERVER_JAR);

        System.out.println("Starting " + serverType + " " + serverVersion + " server on " + path + "...");
        return new ThrowableServer(this.serverInstantiator.startServer(Paths.get(path), TARGET_SERVER_JAR, DockerUtilities.getJavaVersion(serverVersion)));
    }
}
