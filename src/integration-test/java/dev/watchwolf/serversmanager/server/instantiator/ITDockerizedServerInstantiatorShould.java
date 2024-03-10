package dev.watchwolf.serversmanager.server.instantiator;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ITDockerizedServerInstantiatorShould {
    private static final int DEFAULT_PORT = 8001;

    /**
     * For each Docker container, what's the increment to DEFAULT_PORT?
     * We use 2 ports: one for the minecraft server, the next one for WW-Server socket
     */
    private static final int PORT_INCREMENT = 2;



    //
    // DockerClient singletone section
    //

    private static DockerClient dockerClient;
    private synchronized static DockerClient getDockerClient() {
        if (ITDockerizedServerInstantiatorShould.dockerClient == null) {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            ITDockerizedServerInstantiatorShould.dockerClient = DockerClientBuilder.getInstance(config).build();
        }
        return ITDockerizedServerInstantiatorShould.dockerClient;
    }


    //
    // Helper methods
    //

    private static CreateContainerResponse createMcServerContainer(String cmd) {
        String serverId = "MC_Server-" + System.currentTimeMillis();

        CreateContainerResponse container = getDockerClient().createContainerCmd("openjdk:8")
                .withName(serverId)
                .withEntrypoint("/bin/sh", "-c")
                .withCmd(cmd).exec();

        getDockerClient().startContainerCmd(container.getId()).exec();

        return container;
    }

    private static CreateContainerResponse createDummyMcServerContainer() {
        return createMcServerContainer("sleep 20m");
    }

    private static void killContainer(CreateContainerResponse container) {
        getDockerClient().killContainerCmd(container.getId()).exec();
        getDockerClient().removeContainerCmd(container.getId()).exec();
    }

    private static int getNextServerPort() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = DockerizedServerInstantiator.class.getDeclaredMethod("getNextServerPort");
        method.setAccessible(true);
        return (int)method.invoke(null);
    }

    private static String getStartedServerIp(String dockerId) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = DockerizedServerInstantiator.class.getDeclaredMethod("getStartedServerIp", String.class);
        method.setAccessible(true);
        return (String)method.invoke(null, dockerId);
    }

    private static void attachStdio(CreateContainerResponse container, DockerizedServerInstantiator.StdioCallback callback) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = DockerizedServerInstantiator.class.getDeclaredMethod("attachStdio", DockerClient.class, CreateContainerResponse.class, ResultCallback.class);
        method.setAccessible(true);

        String serverIp = getStartedServerIp(container.getId());
        method.invoke(null, getDockerClient(), container, new DockerizedServerInstantiator.StdioAdapter(serverIp, callback));
    }

    private static int getExpectedPort(int openedConnections) {
        return DEFAULT_PORT+openedConnections*PORT_INCREMENT;
    }


    //
    // Tests
    //

    @Test
    void returnDefaultPortWhenNoDockersStarted() throws Exception {
        assertEquals(DEFAULT_PORT, getNextServerPort());
    }

    @Test
    void returnNextPortWhenOneDockerStarted() throws Exception {
        CreateContainerResponse container = null;
        try {
            container = createDummyMcServerContainer();
            Thread.sleep(15_000);
            assertEquals(getExpectedPort(1), getNextServerPort());
        }
        finally {
            if (container != null) killContainer(container);
        }
    }

    @Test
    void returnFirstPortWhenOneDockerStarted() throws Exception {
        CreateContainerResponse container = null;
        try {
            container = createDummyMcServerContainer();
            Thread.sleep(15_000);
            String ip = getStartedServerIp(container.getId());
            assertEquals(DEFAULT_PORT, ip.split(":")[1]);
        }
        finally {
            if (container != null) killContainer(container);
        }
    }

    @Test
    void attachStdio() throws Exception {
        CreateContainerResponse container = null;
        try {
            final StringBuilder sb = new StringBuilder();
            container = createMcServerContainer("echo 'Test 1' ; echo 'Test 2' ; sleep 20m");
            attachStdio(container, (line,stderr) -> sb.append(line).append('\n'));

            String expected = "Test 1\nTest 2\n";

            // wait for the data
            int timeout = 15_000,
                delta = 100;
            int waitingFor = 0;

            while (waitingFor < timeout) {
                if (sb.length() >= expected.length()) break; // we're done

                Thread.sleep(delta);
                waitingFor += delta;
            }
            Thread.sleep(delta); // just in case, wait a bit more (in case we get more data)

            // assert
            assertEquals(expected, sb.toString());
        }
        finally {
            if (container != null) killContainer(container);
        }
    }
}
