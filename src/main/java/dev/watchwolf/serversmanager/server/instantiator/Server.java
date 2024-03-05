package dev.watchwolf.serversmanager.server.instantiator;

import dev.watchwolf.core.rpc.stubs.serversmanager.ServerStartedEvent;
import dev.watchwolf.server.ServerStopNotifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class Server {
    private final String ip;
    protected final Collection<ServerStartedEvent> serverStartedListeners;
    protected final Collection<ServerStopNotifier> serverStoppedListeners;
    protected final Collection<ServerMessageEvent> serverMessageListeners;

    public Server(String ip) {
        this.ip = ip;
        this.serverStartedListeners = new ArrayList<>();
        this.serverStoppedListeners = new ArrayList<>();
        this.serverMessageListeners = new ArrayList<>();
    }

    void raiseServerStartedEvent() throws IOException {
        for (ServerStartedEvent e : this.serverStartedListeners) e.serverStarted();
    }

    void raiseServerStoppedEvent() {
        for (ServerStopNotifier e : this.serverStoppedListeners) e.onServerStop();
    }

    void raiseServerMessageEvent(String msg) {
        for (ServerMessageEvent e : this.serverMessageListeners) e.onMessageEvent(msg);
    }

    public String getIp() {
        return this.ip;
    }
    
    public Server subscribeToServerStartedEvents(ServerStartedEvent subscriber) {
        this.serverStartedListeners.add(subscriber);
        return this;
    }

    public Server subscribeToServerStoppedEvents(ServerStopNotifier subscriber) {
        this.serverStoppedListeners.add(subscriber);
        return this;
    }

    public Server subscribeToServerMessageEvents(ServerMessageEvent subscriber) {
        this.serverMessageListeners.add(subscriber);
        return this;
    }
}
