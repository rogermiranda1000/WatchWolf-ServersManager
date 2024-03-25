package dev.watchwolf.serversmanager.server.plugins;

import dev.watchwolf.core.entities.files.plugins.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface PluginDeserializer {
    /**
     * Places a plugin into a folder
     * @param plugin Plugin to deserialize
     * @param outDirectory Target directory
     */
    void deserialize(Plugin plugin, File outDirectory) throws IOException,UnableToAchievePluginException;

    /**
     * Returns the path of all the usual plugins
     * @return Usual plugins location
     */
    Path getUsualPluginsPath() throws IOException;
}
