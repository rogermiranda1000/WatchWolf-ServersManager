package dev.watchwolf.serversmanager.server.instantiator;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ITDockerizedServerInstantiatorShould {
    private static final int DEFAULT_PORT = DockerizedServerInstantiator.BASE_PORT;

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

    private static CreateContainerResponse createMcServerContainer(String cmd, int ...ports) {
        String serverId = "MC_Server-" + System.currentTimeMillis();

        ArrayList<PortBinding> bindings = new ArrayList<>();
        for (int port : ports) bindings.add(PortBinding.parse(port + ":" + port));

        CreateContainerResponse container = getDockerClient().createContainerCmd("openjdk:8")
                .withName(serverId)
                .withEntrypoint("/bin/sh", "-c")
                .withPortBindings(bindings)
                .withCmd(cmd).exec();

        getDockerClient().startContainerCmd(container.getId()).exec();

        return container;
    }

    private static CreateContainerResponse createDummyMcServerContainer(int ...ports) {
        return createMcServerContainer("sleep 20m", ports);
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
            container = createDummyMcServerContainer(DEFAULT_PORT,DEFAULT_PORT+1);
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
            container = createDummyMcServerContainer(DEFAULT_PORT,DEFAULT_PORT+1);
            Thread.sleep(15_000);
            String ip = getStartedServerIp(container.getId());
            assertEquals(DEFAULT_PORT, ip.split(":")[1]);
        }
        finally {
            if (container != null) killContainer(container);
        }
    }

    @Test
    void returnFirstPortWhenDockerEnded() throws Exception {
        CreateContainerResponse container = null;
        try {
            container = createDummyMcServerContainer(DEFAULT_PORT,DEFAULT_PORT+1);
            Thread.sleep(15_000);
        }
        finally {
            if (container != null) killContainer(container);
        }

        assertEquals(DEFAULT_PORT, getNextServerPort());
    }

    @Test
    void returnFirstPortWhenFirstDockerEnded() throws Exception {
        CreateContainerResponse container = null,
                                secondContainer = null;
        int nextPort = -1;
        try {
            container = createDummyMcServerContainer(DEFAULT_PORT,DEFAULT_PORT+1);
            Thread.sleep(8_000);
            secondContainer = createDummyMcServerContainer(DEFAULT_PORT+2,DEFAULT_PORT+3);
            Thread.sleep(15_000);

            // now both are ready; stop the first one
            killContainer(container);
            container = null;

            nextPort = getNextServerPort();
        }
        finally {
            if (container != null) killContainer(container);
            if (secondContainer != null) killContainer(secondContainer);
        }

        assertEquals(DEFAULT_PORT, nextPort);
    }

    @Test
    void attachStdio() throws Exception {
        CreateContainerResponse container = null;
        try {
            final StringBuilder sb = new StringBuilder();
            container = createMcServerContainer("echo 'Test 1' ; echo 'Test 2' ; sleep 20m", DEFAULT_PORT,DEFAULT_PORT+1);
            attachStdio(container, (line,stderr) -> sb.append(line).append('\n'));

            String expected = "Test 1\nTest 2\n"; // the \n are because we're appending them on the StdioCallback

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
