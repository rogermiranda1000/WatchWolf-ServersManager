package dev.watchwolf.serversmanager.server;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.ZipFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.entities.files.plugins.PluginFactory;
import dev.watchwolf.serversmanager.server.plugins.PluginDeserializer;
import dev.watchwolf.serversmanager.server.plugins.ServersManagerPluginDeserializer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class ServerRequirements {
    private static PluginDeserializer deserializer = new ServersManagerPluginDeserializer();

    public static String setupFolder(String serverType, String serverVersion, Collection<Plugin> plugins, WorldType worldType, Collection<ConfigFile> maps, Collection<ConfigFile> configFiles) throws IOException {
        String serverFolder = "."; // TODO get tmp folder

        // TODO copy server type&version
        // TODO setup server config (worldType and other parameters)

        // export worlds
        for (ConfigFile map : maps) {
            if (!(map instanceof ZipFile)) throw new IllegalArgumentException("All worlds must be zips; got `." + map.getExtension() + "` instead.");
            ((ZipFile)map).exportToDirectory(new File(serverFolder));
        }

        // export plugins
        String basePluginsFolder = serverFolder + "/plugins/";
        for (Plugin plugin : plugins) {
            deserializer.deserialize(plugin, new File(basePluginsFolder));
        }

        // export config files
        for (ConfigFile configFile : configFiles) {
            if (configFile.getOffsetPath().contains("../") || configFile.getOffsetPath().startsWith("/")) {
                System.err.println("Got file with illegal offset (" + configFile.getOffsetPath() + "); will ignore.");
                continue;
            }

            if (configFile instanceof ZipFile) ((ZipFile)configFile).exportToDirectory(new File(basePluginsFolder + configFile.getOffsetPath()));
            else configFile.saveToFile(new File(basePluginsFolder + configFile.getOffsetPath() + configFile.getName() + "." + configFile.getExtension()));
        }

        return serverFolder;
    }
}
