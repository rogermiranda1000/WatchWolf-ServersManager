package dev.watchwolf.serversmanager.server.plugins;

import dev.watchwolf.core.entities.Version;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ServersManagerPluginDeserializer implements PluginDeserializer {
    private static final Path usualPluginsFolder = Paths.get( (System.getenv("SERVER_PATH_SHIFT") == null) ? "." : System.getenv("SERVER_PATH_SHIFT") ).resolve("usual-plugins");

    private Logger logger = LogManager.getLogger(ServersManagerPluginDeserializer.class.getName());

    /**
     * Places a plugin into a folder
     * @param plugin Plugin to deserialize
     * @param outDirectory Target directory
     */
    @Override
    public void deserialize(Plugin plugin, File outDirectory) throws IOException,UnableToAchievePluginException {
        logger.traceEntry(null, plugin, outDirectory);
        if (plugin instanceof FilePlugin) {
            ConfigFile filePlugin = ((FilePlugin)plugin).getFile();
            logger.debug("Got FilePlugin: " + filePlugin.toString());
            filePlugin.saveToFile(new File(outDirectory, filePlugin.getName() + "." + filePlugin.getExtension()));
        }
        else if (plugin instanceof UsualPlugin) {
            UsualPlugin usualPlugin = (UsualPlugin)plugin;
            logger.debug("Got UsualPlugin: " + usualPlugin.toString());
            if (usualPlugin.getVersion() == null) throw logger.throwing(new IllegalArgumentException("Requested usual plugin " + usualPlugin.getName() + ", but no version given"));
            // TODO add
        }
        else if (plugin instanceof UploadedPlugin) {
            UploadedPlugin uploadedPlugin = (UploadedPlugin)plugin;
            logger.debug("Got UploadedPlugin: " + uploadedPlugin.toString());
            // TODO
        }
        else throw logger.throwing(new IllegalArgumentException("Couldn't deserialize plugin of type " + plugin.getClass().getName()));
        logger.traceExit();
    }

    /**
     * Returns the plugins according to the target mc version
     * @param plugins Plugins
     * @param targetMcVersion Server version
     * @return Plugins that can be added, with the required data added
     */
    @Override
    public List<Plugin> filterByVersion(List<Plugin> plugins, String targetMcVersion) throws IOException {
        this.logger.traceEntry(null, plugins, targetMcVersion);
        List<Plugin> filteredPlugins = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (!(plugin instanceof UsualPlugin)) filteredPlugins.add(plugin); // independent to the MC version
            else {
                UsualPlugin usualPlugin = (UsualPlugin) plugin;
                List<Version> compatibleVersions = getCompatibleVersions(usualPlugin.getName(), targetMcVersion);
                if (usualPlugin.getVersion() != null) {
                    // version specified; check if compatible
                    if (!compatibleVersions.contains(new Version(usualPlugin.getVersion()))) {
                        this.logger.warn("Got usual plugin with specified version (" + usualPlugin.getName() + ", v" + usualPlugin.getVersion() + "), but that version is incompatible or was not found in the usual plugins folder. Ignoring plugin...");
                    }
                    else filteredPlugins.add(usualPlugin); // all ok
                }
                else {
                    // we have to add the version for the `deserialize` method
                    try {
                        Version usualPluginHighestCompatibleVersion = Collections.max(getCompatibleVersions(usualPlugin.getName(), targetMcVersion));
                        filteredPlugins.add(new UsualPlugin(usualPlugin.getName(), usualPluginHighestCompatibleVersion.toString(), usualPlugin.isPremium())); // add the plugin with the specific version
                    } catch (NoSuchElementException ignore) {
                        this.logger.warn("Got usual plugin " + usualPlugin.getName() + ", but no compatible version was found for Minecraft " + targetMcVersion + ". Ignoring plugin...");
                    }
                }
            }
        }
        return this.logger.traceExit(filteredPlugins);
    }

    /**
     * Checks all the available versions of a usual plugin and returns only the ones that
     * are compatible with the specified minecraft server version.
     * @param usualPlugin Usual plugin name to check
     * @param minecraftServerVersion MC server version
     * @return Compatible plugin versions with the specified MC version
     */
    public List<Version> getCompatibleVersions(final String usualPlugin, String minecraftServerVersion) throws IOException {
        this.logger.traceEntry(null, usualPlugin, minecraftServerVersion);
        Version mcVersion = new Version(minecraftServerVersion);
        List<String> plugins = Files.list(this.getUsualPluginsPath())
                                    .filter(path -> !Files.isDirectory(path))
                                    .map(path -> path.getFileName().toString())
                                    .filter(file -> file.endsWith(".jar")) // only plugins
                                    .filter(plugin -> plugin.startsWith(usualPlugin)) // only this plugin
                                    .toList();
        List<Version> compatibleVersions = new ArrayList<>();
        Pattern pluginPattern = Pattern.compile("^([^-]+)-([\\d.]+)-([\\d.]+)-((\\d+(\\.\\d+)*)|(LATEST))\\.jar$");
        for (String plugin : plugins) {
            Matcher m = pluginPattern.matcher(plugin);
            if (!m.find()) {
                this.logger.warn("Usual plugin file '" + plugin + "' didn't match the regex");
                continue;
            }

            String pluginName = m.group(1);
            if (!pluginName.equals(usualPlugin)) continue; // different plugin
            Version pluginVersion = new Version(m.group(2));
            Version minMcVersionForCurrentPluginVersion = new Version(m.group(3)),
                    maxMcVersionForCurrentPluginVersion = (m.group(4).equals("LATEST") ? null : new Version(m.group(4)));

            if (mcVersion.compareTo(minMcVersionForCurrentPluginVersion) >= 0 && (maxMcVersionForCurrentPluginVersion == null || mcVersion.compareTo(maxMcVersionForCurrentPluginVersion) <= 0)) {
                // match!
                compatibleVersions.add(pluginVersion);
            }
        }
        return this.logger.traceExit(compatibleVersions);
    }

    /**
     * Returns the path of all the usual plugins
     * @return Usual plugins location
     */
    @Override
    public Path getUsualPluginsPath() {
        return this.logger.traceExit(usualPluginsFolder);
    }
}
