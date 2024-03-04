package dev.watchwolf.serversmanager.server;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

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
}
