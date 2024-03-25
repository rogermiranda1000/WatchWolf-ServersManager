package dev.watchwolf.serversmanager.server;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import dev.watchwolf.serversmanager.server.plugins.PluginDeserializer;
import dev.watchwolf.serversmanager.server.plugins.ServersManagerPluginDeserializer;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerRequirementsShould {
    public static final String TARGET_SERVER_JAR = "ServersManager.jar";

    //  =======================
    //    copyServerJar tests
    //  =======================


    private static void setServerTypesFolder(Path sourcePath) throws NoSuchFieldException,IllegalAccessException {
        Field deserializerField = ServerRequirements.class.getDeclaredField("serverTypesFolder");
        deserializerField.setAccessible(true);
        deserializerField.set(null, sourcePath);
    }

    private static void clearServerTypesFolder() throws NoSuchFieldException,IllegalAccessException {
        setServerTypesFolder(Paths.get(".")); // as tests run without env variables, it will be "."
    }


    private static Method getCopyServerJarMethod() throws NoSuchMethodException {
        Method method = ServerRequirements.class.getDeclaredMethod("copyServerJar", String.class, String.class, Path.class, String.class);
        method.setAccessible(true);
        return method;
    }

    private static Path givenAPathWithOneSpigot1_20ServerAndOnePaper1_20Server(FileSystem fileSystem) throws IOException {
        Path sourcePath = fileSystem.getPath("/server-types");
        String version = "1.20";
        for (String serverType : new String[]{"Spigot", "Paper"}) {
            Path serversFolder = sourcePath.resolve(serverType),
                    serverFile = serversFolder.resolve(version + ".jar");
            String fileContents = serverType + " " + version;

            Files.createDirectories(serversFolder);
            Files.write(serverFile, fileContents.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        }
        return sourcePath;
    }

    private static Path givenAPathWithOneSpigot1_20And1_8_8ServerAndOnePaper1_19Server(FileSystem fileSystem) throws IOException {
        Path sourcePath = fileSystem.getPath("/server-types");

        Files.createDirectories(sourcePath.resolve("Spigot"));
        Files.createFile(sourcePath.resolve("Spigot").resolve("1.20.jar"));
        Files.createFile(sourcePath.resolve("Spigot").resolve("1.8.8.jar"));

        Files.createDirectories(sourcePath.resolve("Paper"));
        Files.createFile(sourcePath.resolve("Paper").resolve("1.19.jar"));

        return sourcePath;
    }

    private static Path givenACreatedDestinyFolder(FileSystem fileSystem) throws IOException {
        Path dstPath = fileSystem.getPath("/dst");
        Files.createDirectories(dstPath);
        return dstPath;
    }

    @Test
    void placeTheAppropriateServerJarInTheRootTargetFolder() throws Exception {
        try {
            FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
            Path sourcePath = givenAPathWithOneSpigot1_20ServerAndOnePaper1_20Server(fileSystem),
                    dstPath = givenACreatedDestinyFolder(fileSystem);
            Path outFile = dstPath.resolve(TARGET_SERVER_JAR);

            setServerTypesFolder(sourcePath);

            // act
            getCopyServerJarMethod().invoke(null, "Spigot", "1.20", dstPath, TARGET_SERVER_JAR);

            // assert
            assertTrue(Files.exists(outFile)); // the file should exist
            assertEquals("Spigot 1.20", Files.readAllLines(outFile).get(0)); // `givenAPathWithOneSpigot1_20ServerAndOnePaper1_20Server` is writing the server type and version
        } finally {
            clearServerTypesFolder();
        }
    }

    @Test
    void raiseAnExceptionIfExactVersionIsNotPresent() throws Throwable {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        Path sourcePath = givenAPathWithOneSpigot1_20ServerAndOnePaper1_20Server(fileSystem),
                dstPath = givenACreatedDestinyFolder(fileSystem);

        try {
            setServerTypesFolder(sourcePath);

            getCopyServerJarMethod().invoke(null, "Spigot", "1.19", dstPath, TARGET_SERVER_JAR);
        } catch (Throwable ex) {
            // we expect a `ServerJarUnavailableException`
            if (ex instanceof InvocationTargetException) ex = ((InvocationTargetException)ex).getCause();
            if (!ex.getClass().equals(ServerJarUnavailableException.class)) throw ex;
        } finally {
            clearServerTypesFolder();
        }
    }



    //  ==========================
    //    generateEulaFile tests
    //  ==========================


    private static Method getGenerateEulaFileMethod() throws NoSuchMethodException {
        Method method = ServerRequirements.class.getDeclaredMethod("generateEulaFile", Path.class);
        method.setAccessible(true);
        return method;
    }

    @Test
    void generateEulaFile() throws Exception {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        Path dstPath = givenACreatedDestinyFolder(fileSystem);
        Path outFile = dstPath.resolve("eula.txt");

        // act
        getGenerateEulaFileMethod().invoke(null, dstPath);

        // assert
        assertTrue(Files.exists(outFile)); // the file should exist
        assertEquals("eula=true", Files.readAllLines(outFile).get(0)); // eula should be accepted
    }



    //  =====================
    //    setupFolder tests
    //  =====================


    private static void setDeserializer(PluginDeserializer pluginDeserializer) throws NoSuchFieldException,IllegalAccessException {
        Field deserializerField = ServerRequirements.class.getDeclaredField("deserializer");
        deserializerField.setAccessible(true);

        // unset 'final' modifier
        /*Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(deserializerField, deserializerField.getModifiers() & ~Modifier.FINAL);*/

        deserializerField.set(null, pluginDeserializer);
    }

    private static void setPluginDeserializerMock(Path usualPluginsFolder) throws IOException, NoSuchFieldException, IllegalAccessException {
        Files.createDirectory(usualPluginsFolder);

        // create mock to point to created folder
        PluginDeserializer pluginDeserializer = mock(PluginDeserializer.class);
        when(pluginDeserializer.getUsualPluginsPath())
                .thenReturn(usualPluginsFolder);

        setDeserializer(pluginDeserializer); // set mock as deserializer
    }

    private static void clearPluginDeserializerMock() throws NoSuchFieldException, IllegalAccessException {
        setDeserializer(new ServersManagerPluginDeserializer());
    }

    private static void disableShowServerFolderInfo() throws NoSuchFieldException, IllegalAccessException {
        Field deserializerField = ServerRequirements.class.getDeclaredField("serverFolderInfoLogged");
        deserializerField.setAccessible(true);
        deserializerField.set(null, true);
    }

    @Test
    @Disabled
    void prepareAServerFolder() throws Exception {
        try (MockedStatic<ServerRequirements> dummyStatic = Mockito.mockStatic(ServerRequirements.class,Mockito.CALLS_REAL_METHODS)) {
            // arrange
            disableShowServerFolderInfo(); // otherwise it will fail because there's no servers

            String serverType = "Spigot";
            String serverVersion = "1.20";
            Collection<Plugin> plugins = new ArrayList<>();
            WorldType worldType = WorldType.FLAT;
            Collection<ConfigFile> maps = new ArrayList<>();
            Collection<ConfigFile> configFiles = new ArrayList<>();
            String jarName = TARGET_SERVER_JAR;

            FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
            Path sourcePath = givenAPathWithOneSpigot1_20ServerAndOnePaper1_20Server(fileSystem),
                    dstPath = givenACreatedDestinyFolder(fileSystem);
            Path expectedJar = dstPath.resolve(TARGET_SERVER_JAR);

            // mocking
            dummyStatic.when(ServerRequirements::createServerFolder)
                    .thenReturn(dstPath);
            dummyStatic.when(() -> ServerRequirements.getGlobalServerFolder(anyString()))
                    .thenReturn(""); // we don't care about the return

            setServerTypesFolder(sourcePath);

            // act
            ServerRequirements.setupFolder(serverType, serverVersion, plugins, worldType, maps, configFiles, jarName);

            // assert
            assertTrue(Files.exists(expectedJar));
            // TODO add verify calls to each of the expected methods to call
        } finally {
            clearServerTypesFolder();
        }
    }

    @Test
    @Disabled
    void logAllPluginsAvailable() throws Exception {
        // arrange
        try(FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
            LogCaptor logCaptor = LogCaptor.forClass(ServerRequirements.class)) {
            Path serverTypesFolder = fs.getPath("/servers/server-types");
            Files.createDirectories(serverTypesFolder);
            setServerTypesFolder(serverTypesFolder); // we need the mock or it will crash

            Path usualPluginsFolder = fs.getPath("/plugins");
            setPluginDeserializerMock(usualPluginsFolder);

            // create the usual-plugins
            Files.createFile(usualPluginsFolder.resolve("WatchWolf-0.1-1.8-LATEST.jar"));
            Files.createFile(usualPluginsFolder.resolve("MyValuablePlugin-1.0-1.8-LATEST.jar"));

            // act
            ServerRequirements.logServerFolderInfo();

            // assert
            assertTrue(logCaptor.getLogs().contains("Usual plugins: [WatchWolf-0.1-1.8-LATEST.jar, MyValuablePlugin-1.0-1.8-LATEST.jar]"),
                    "Expected ServerRequirements to log the usual plugins on the folder ('WatchWolf' and 'MyValuablePlugin'), but got different data.\nCaptured logs:\n" + logCaptor.getLogs().toString());
        } finally {
            clearPluginDeserializerMock();
            clearServerTypesFolder();
        }
    }

    @Test
    @Disabled
    void logAllServersAvailable() throws Exception {
        // arrange
        try(FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
            LogCaptor logCaptor = LogCaptor.forClass(ServerRequirements.class)) {
            Path usualPluginsFolder = fs.getPath("/plugins");
            setPluginDeserializerMock(usualPluginsFolder); // we need the mock or it will crash

            setServerTypesFolder(givenAPathWithOneSpigot1_20And1_8_8ServerAndOnePaper1_19Server(fs));

            // act
            ServerRequirements.logServerFolderInfo();

            // assert
            assertTrue(logCaptor.getLogs().contains("Servers available: {Paper=[1.19], Spigot=[1.20, 1.8.8]}"),
                    "Expected ServerRequirements to log the servers available (Spigot 1.20, Spigot 1.8.8 and Paper 1.19), but got different data.\nCaptured logs:\n" + logCaptor.getLogs().toString());
        } finally {
            clearPluginDeserializerMock();
            clearServerTypesFolder();
        }
    }
}
