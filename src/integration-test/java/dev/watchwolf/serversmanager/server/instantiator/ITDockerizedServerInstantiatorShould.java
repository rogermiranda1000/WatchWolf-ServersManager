package dev.watchwolf.serversmanager.server.instantiator;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
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

    private static DockerClient dockerClient;
    private synchronized static DockerClient getDockerClient() {
        if (ITDockerizedServerInstantiatorShould.dockerClient == null) {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            ITDockerizedServerInstantiatorShould.dockerClient = DockerClientBuilder.getInstance(config).build();
        }
        return ITDockerizedServerInstantiatorShould.dockerClient;
    }

    private static CreateContainerResponse createDummyMcServerContainer() {
        String serverId = "MC_Server-" + System.currentTimeMillis();

        CreateContainerResponse container = getDockerClient().createContainerCmd("openjdk:8")
                .withName(serverId)
                .withEntrypoint("/bin/sh", "-c")
                .withCmd("sleep 20m").exec();

        getDockerClient().startContainerCmd(container.getId()).exec();

        return container;
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

    private static int getExpectedPort(int openedConnections) {
        return DEFAULT_PORT+openedConnections*PORT_INCREMENT;
    }

    @Test
    void returnDefaultPortIfNoDockersStarted() throws Exception {
        assertEquals(DEFAULT_PORT, getNextServerPort());
    }

    @Test
    void returnNextPortIfOneDockersStarted() throws Exception {
        CreateContainerResponse container = null;
        try {
            container = createDummyMcServerContainer();
            Thread.sleep(30_000);
            assertEquals(getExpectedPort(1), getNextServerPort());
        }
        finally {
            if (container != null) killContainer(container);
        }

    }
}
