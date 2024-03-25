package dev.watchwolf.serversmanager.server.plugins;

import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServersManagerPluginDeserializer implements PluginDeserializer {
    private static final Path usualPluginsFolder = Paths.get( (System.getenv("SERVER_PATH_SHIFT") == null) ? "." : System.getenv("SERVER_PATH_SHIFT") ).resolve("usual-plugins");

    /**
     * Places a plugin into a folder
     * @param plugin Plugin to deserialize
     * @param outDirectory Target directory
     */
    @Override
    public void deserialize(Plugin plugin, File outDirectory) throws IOException,UnableToAchievePluginException {
        if (plugin instanceof FilePlugin) {
            ConfigFile filePlugin = ((FilePlugin)plugin).getFile();
            filePlugin.saveToFile(new File(outDirectory, filePlugin.getName() + "." + filePlugin.getExtension()));
        }
        else if (plugin instanceof UsualPlugin); // TODO
        else if (plugin instanceof UploadedPlugin); // TODO
        else throw new IllegalArgumentException("Couldn't deserialize plugin of type " + plugin.getClass().getName());
    }

    /**
     * Returns the path of all the usual plugins
     * @return Usual plugins location
     */
    @Override
    public Path getUsualPluginsPath() throws IOException {
        return usualPluginsFolder;
    }
}
