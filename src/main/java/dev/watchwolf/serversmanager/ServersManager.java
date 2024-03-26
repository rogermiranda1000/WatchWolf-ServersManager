package dev.watchwolf.serversmanager;

import dev.watchwolf.core.rpc.RPC;
import dev.watchwolf.core.rpc.RPCFactory;
import dev.watchwolf.core.rpc.channel.MessageChannel;
import dev.watchwolf.core.rpc.channel.sockets.server.ServerSocketChannelFactory;
import dev.watchwolf.serversmanager.rpc.ServersManagerLocalFactory;
import dev.watchwolf.serversmanager.server.instantiator.DockerizedServerInstantiator;

import java.io.IOException;
import java.net.BindException;

public class ServersManager {
    /**
     * Use port 8000 for Servers Manager
     */
    public static final int SERVERS_MANAGER_PORT = (DockerizedServerInstantiator.BASE_PORT - 1);

    private static boolean started = false;
    private static MessageChannel serverSocketChannel = null;

    private static synchronized boolean isStarted() {
        return ServersManager.started;
    }

    public static void main(String[] args) {
        synchronized (ServersManager.class) {
            if (isStarted()) throw new RuntimeException("ServersManager is already running!");
            ServersManager.started = true;
        }

        RPC rpcMaster = null;
        try {
            rpcMaster = new RPCFactory().build(new ServersManagerLocalFactory(), new ServerSocketChannelFactory("127.0.0.1", SERVERS_MANAGER_PORT));
            synchronized (ServersManager.class) {
                serverSocketChannel = rpcMaster._getRemoteConnection();
            }
            rpcMaster.run();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        while (isStarted()) {
            RPC serversManager = new RPCFactory().build(new ServersManagerLocalFactory(), rpcMaster);
            serversManager.run();
        }
    }

    // TODO call on ctrl-c
    public static synchronized void stop() {
        ServersManager.started = false;

        // closing the server will cause all the `run()` to get unstuck
        try {
            if (serverSocketChannel != null) serverSocketChannel.close();
        } catch (IOException ignore) {}
    }
}
