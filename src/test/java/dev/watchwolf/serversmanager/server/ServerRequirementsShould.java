package dev.watchwolf.serversmanager.server;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import dev.watchwolf.core.entities.WorldType;
import dev.watchwolf.core.entities.files.ConfigFile;
import dev.watchwolf.core.entities.files.plugins.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

public class ServerRequirementsShould {
    public static final String TARGET_SERVER_JAR = "ServersManager.jar";

    //  =======================
    //    copyServerJar tests
    //  =======================


    private static Method getCopyServerJarMethod() throws NoSuchMethodException {
        Method method = ServerRequirements.class.getDeclaredMethod("copyServerJar", String.class, String.class, Path.class, Path.class, String.class);
        method.setAccessible(true);
        return method;
    }

    private static Path givenAPathWithOneSpigot1_20ServerAndOnePaper1_20Server(FileSystem fileSystem) throws IOException {
        Path sourcePath = fileSystem.getPath("/src");
        String version = "1.20";
        for (String serverType : new String[]{"Spigot", "Paper"}) {
            Path serversFolder = sourcePath.resolve("server-types/" + serverType),
                    serverFile = serversFolder.resolve(version + ".jar");
            String fileContents = serverType + " " + version;

            Files.createDirectories(serversFolder);
            Files.write(serverFile, fileContents.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        }
        return sourcePath;
    }

    private static Path givenACreatedDestinyFolder(FileSystem fileSystem) throws IOException {
        Path dstPath = fileSystem.getPath("/dst");
        Files.createDirectories(dstPath);
        return dstPath;
    }

    @Test
    void placeTheAppropriateServerJarInTheRootTargetFolder() throws Exception {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        Path sourcePath = givenAPathWithOneSpigot1_20ServerAndOnePaper1_20Server(fileSystem),
                dstPath = givenACreatedDestinyFolder(fileSystem);
        Path outFile = dstPath.resolve(TARGET_SERVER_JAR);

        // act
        getCopyServerJarMethod().invoke(null, "Spigot", "1.20", sourcePath, dstPath, TARGET_SERVER_JAR);

        // assert
        assertTrue(Files.exists(outFile)); // the file should exist
        assertEquals("Spigot 1.20", Files.readAllLines(outFile).get(0)); // `givenAPathWithOneSpigot1_20ServerAndOnePaper1_20Server` is writing the server type and version
    }

    @Test
    void raiseAnExceptionIfExactVersionIsNotPresent() throws Throwable {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        Path sourcePath = givenAPathWithOneSpigot1_20ServerAndOnePaper1_20Server(fileSystem),
                dstPath = givenACreatedDestinyFolder(fileSystem);

        try {
            getCopyServerJarMethod().invoke(null, "Spigot", "1.19", sourcePath, dstPath, TARGET_SERVER_JAR);
        } catch (Throwable ex) {
            // we expect a `ServerJarUnavailableException`
            if (ex instanceof InvocationTargetException) ex = ((InvocationTargetException)ex).getCause();
            if (!ex.getClass().equals(ServerJarUnavailableException.class)) throw ex;
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


    @Test
    void prepareAServerFolder() throws Exception {
        try (MockedStatic<ServerRequirements> dummyStatic = Mockito.mockStatic(ServerRequirements.class,Mockito.CALLS_REAL_METHODS)) {
            // arrange
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

            // act
            ServerRequirements.setupFolder(serverType, serverVersion, plugins, worldType, maps, configFiles, jarName, sourcePath);

            // assert
            assertTrue(Files.exists(expectedJar));
            // TODO add verify calls to each of the expected methods to call
        }
    }
}
