package dev.watchwolf.serversmanager.server;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.ZipFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.entities.files.plugins.UsualPlugin;
import dev.watchwolf.serversmanager.server.plugins.PluginDeserializer;
import dev.watchwolf.serversmanager.server.plugins.ServersManagerPluginDeserializer;
import dev.watchwolf.serversmanager.server.plugins.UnableToAchievePluginException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerRequirements {
    public static final String SHARED_TMP_FOLDER = "{pwd}/tmp";

    private static final PluginDeserializer deserializer = new ServersManagerPluginDeserializer();

    private static void copyServerJar(String serverType, String serverVersion, Path baseFolder, Path targetFolder, String jarName) throws ServerJarUnavailableException,IOException {
        Path serverJar = baseFolder.resolve("server-types/" + serverType + "/" + serverVersion + ".jar");
        if (!Files.exists(serverJar)) throw new ServerJarUnavailableException("Couldn't find " + serverType + " " + serverVersion + " on expected location (" + serverJar.toString() + ")");

        Files.copy(serverJar, targetFolder.resolve(jarName));
    }

    private static String createServerFolder() throws IOException {
        String serverFolder = SHARED_TMP_FOLDER.replace("{pwd}", ".") + "/" + System.currentTimeMillis();
        Files.createDirectories(new File(serverFolder).toPath());
        return serverFolder;
    }

    private static String getGlobalServerFolder(String localServerFolder) {
        String tmpFolderBase = SHARED_TMP_FOLDER;
        if (ServerRequirements.SHARED_TMP_FOLDER.contains("{pwd}")) {
            if (System.getenv("PARENT_PWD") == null) throw new NullPointerException("env variable PARENT_PWD undefined");
            tmpFolderBase = tmpFolderBase.replace("{pwd}", System.getenv("PARENT_PWD"));
        }

        return tmpFolderBase + "/" + getHashFromServerPath(localServerFolder);
    }

    public static String setupFolder(String serverType, String serverVersion, Collection<Plugin> plugins, WorldType worldType, Collection<ConfigFile> maps, Collection<ConfigFile> configFiles, String jarName) throws IOException {
        String serverFolder = ServerRequirements.createServerFolder();

        // copy server (type&version)
        ServerRequirements.copyServerJar(serverType, serverVersion, Paths.get("."), Paths.get(serverFolder), jarName);
        // TODO setup server config (worldType and other parameters)

        // export worlds
        for (ConfigFile map : maps) {
            if (!(map instanceof ZipFile)) throw new IllegalArgumentException("All worlds must be zips; got `." + map.getExtension() + "` instead.");
            ((ZipFile)map).exportToDirectory(new File(serverFolder));
        }

        // export plugins
        String basePluginsFolder = serverFolder + "/plugins";
        for (Plugin plugin : plugins) {
            try {
                deserializer.deserialize(plugin, new File(basePluginsFolder));
            }  catch (UnableToAchievePluginException ex) {
                System.err.println("Couldn't find plugin " + plugin.toString());
                // keep going; if the plugin was required WW-Tester will stop
            }
        }
        deserializer.deserialize(new UsualPlugin("WatchWolf"), new File(basePluginsFolder)); // always add WW

        // export config files
        for (ConfigFile configFile : configFiles) {
            if (configFile.getOffsetPath().contains("../") || configFile.getOffsetPath().startsWith("/")) {
                System.err.println("Got file with illegal offset (" + configFile.getOffsetPath() + "); will ignore.");
                continue;
            }

            if (configFile instanceof ZipFile) ((ZipFile)configFile).exportToDirectory(new File(basePluginsFolder + configFile.getOffsetPath()));
            else configFile.saveToFile(new File(basePluginsFolder + "/" + configFile.getOffsetPath() + configFile.getName() + "." + configFile.getExtension()));
        }
        // TODO add WW-Server config file

        // we must return the global folder
        return getGlobalServerFolder(serverFolder);
    }

    public static String getHashFromServerPath(String path) {
        Matcher match = Pattern.compile("/(\\d+)$")
                                .matcher(path);

        if (!match.find()) throw new IllegalArgumentException("We expect .../<hash>; got '" + path + "' instead");
        return match.group(1);
    }
}
