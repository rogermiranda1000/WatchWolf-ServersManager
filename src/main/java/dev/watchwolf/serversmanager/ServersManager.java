package dev.watchwolf.serversmanager;

import dev.watchwolf.core.rpc.RPC;
import dev.watchwolf.core.rpc.RPCFactory;
import dev.watchwolf.core.rpc.channel.sockets.server.ServerSocketChannelFactory;
import dev.watchwolf.serversmanager.rpc.ServersManagerLocalFactory;
import dev.watchwolf.serversmanager.server.instantiator.DockerizedServerInstantiator;

import java.io.IOException;

public class ServersManager {
    /**
     * Use port 8000 for Servers Manager
     */
    public static final int SERVERS_MANAGER_PORT = (DockerizedServerInstantiator.BASE_PORT - 1);
    public static void main(String[] args) throws IOException {
        RPC serversManager = new RPCFactory().build(new ServersManagerLocalFactory(), new ServerSocketChannelFactory("127.0.0.1", SERVERS_MANAGER_PORT));
        serversManager.run();
    }
}
