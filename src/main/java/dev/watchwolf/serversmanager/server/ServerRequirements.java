package dev.watchwolf.serversmanager.server;

import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.ZipFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.core.entities.files.plugins.UsualPlugin;
import dev.watchwolf.serversmanager.server.plugins.PluginDeserializer;
import dev.watchwolf.serversmanager.server.plugins.ServersManagerPluginDeserializer;
import dev.watchwolf.serversmanager.server.plugins.UnableToAchievePluginException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerRequirements {
    public static final String SHARED_TMP_FOLDER = "{pwd}/{offset}/tmp";

    private static final Logger logger = LogManager.getLogger(ServerRequirements.class.getName());
    private static boolean serverFolderInfoLogged = false;

    private static PluginDeserializer deserializer = new ServersManagerPluginDeserializer();
    private static Path serverTypesFolder = Paths.get( (System.getenv("SERVER_PATH_SHIFT") == null) ? "." : System.getenv("SERVER_PATH_SHIFT") ).resolve("server-types");

    private static void copyServerJar(String serverType, String serverVersion, Path targetFolder, String jarName) throws ServerJarUnavailableException,IOException {
        Path serverJar = serverTypesFolder.resolve(serverType + "/" + serverVersion + ".jar");
        if (!Files.exists(serverJar)) throw new ServerJarUnavailableException("Couldn't find " + serverType + " " + serverVersion + " on expected location (" + serverJar.toString() + ")");

        Files.copy(serverJar, targetFolder.resolve(jarName));
    }

    static Path createServerFolder() throws IOException {
        String offset = (System.getenv("SERVER_PATH_SHIFT") == null) ? "." : System.getenv("SERVER_PATH_SHIFT");
        String serverFolder = SHARED_TMP_FOLDER.replace("{pwd}", ".").replace("{offset}", offset) + "/" + System.currentTimeMillis();
        Files.createDirectories(new File(serverFolder).toPath());
        return Paths.get(serverFolder);
    }

    /**
     * To start a Minecraft server it is required that we accept the eula (via a file)
     * @param targetFolder Out folder
     * @throws IOException Failed to create the file
     */
    private static void generateEulaFile(Path targetFolder) throws IOException {
        String acceptEulaString = "eula=true";
        Files.write(targetFolder.resolve("eula.txt"), acceptEulaString.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
    }

    static String getGlobalServerFolder(String localServerFolder) {
        String tmpFolderBase = SHARED_TMP_FOLDER;

        if (ServerRequirements.SHARED_TMP_FOLDER.contains("{pwd}")) {
            if (System.getenv("PARENT_PWD") == null) throw new NullPointerException("env variable PARENT_PWD undefined");
            tmpFolderBase = tmpFolderBase.replace("{pwd}", System.getenv("PARENT_PWD"));
        }

        String offset = (System.getenv("SERVER_PATH_SHIFT") == null) ? "." : System.getenv("SERVER_PATH_SHIFT");
        tmpFolderBase = tmpFolderBase.replace("{offset}", offset);

        return tmpFolderBase + "/" + getHashFromServerPath(localServerFolder);
    }

    public static void logServerFolderInfo() throws IOException {
        // print all usual plugins got
        Path usualPluginsPath = deserializer.getUsualPluginsPath();
        Set<String> usualPlugins = Files.list(usualPluginsPath)
                .filter(file -> !Files.isDirectory(file))
                .map(file -> file.getFileName().toString())
                .filter((name) -> name.endsWith(".jar"))
                .collect(Collectors.toSet());
        ServerRequirements.logger.info("Usual plugins: " + usualPlugins.toString());

        // print all server types got
        Map<String, List<String>> serversAvailable = new HashMap<>();
        for (Path serverType : Files.list(serverTypesFolder).collect(Collectors.toList())) {
            serversAvailable.put(serverType.getFileName().toString(),
                    Files.list(serverType)
                            .map(f -> f.getFileName().toString())
                            .filter(f -> f.endsWith(".jar")) // only servers
                            .map(f -> f.substring(0, f.length() - 4)) // remove extension
                            .collect(Collectors.toList())
            );
        }
        ServerRequirements.logger.info("Servers available: " + serversAvailable.toString());

        // now we've logged the information
        ServerRequirements.serverFolderInfoLogged = true;
    }

    public static String setupFolder(String serverType, String serverVersion, Collection<Plugin> plugins, WorldType worldType, Collection<ConfigFile> maps, Collection<ConfigFile> configFiles, String jarName) throws IOException {
        if (!ServerRequirements.serverFolderInfoLogged) ServerRequirements.logServerFolderInfo();

        Path serverFolder = ServerRequirements.createServerFolder();

        // copy server (type&version)
        ServerRequirements.copyServerJar(serverType, serverVersion, serverFolder, jarName);
        generateEulaFile(serverFolder);
        // TODO setup server config (worldType and other parameters)

        // export worlds
        for (ConfigFile map : maps) {
            if (!(map instanceof ZipFile)) throw new IllegalArgumentException("All worlds must be zips; got `." + map.getExtension() + "` instead.");
            ((ZipFile)map).exportToDirectory(serverFolder);
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

            if (configFile instanceof ZipFile) ((ZipFile)configFile).exportToDirectory(Path.of(basePluginsFolder + configFile.getOffsetPath()));
            else configFile.saveToFile(new File(basePluginsFolder + "/" + configFile.getOffsetPath() + configFile.getName() + "." + configFile.getExtension()));
        }
        // TODO add WW-Server config file

        // we must return the global folder
        return getGlobalServerFolder(serverFolder.toString());
    }

    public static String getHashFromServerPath(String path) {
        Matcher match = Pattern.compile("/(\\d+)$")
                                .matcher(path);

        if (!match.find()) throw new IllegalArgumentException("We expect .../<hash>; got '" + path + "' instead");
        return match.group(1);
    }
}
