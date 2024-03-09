package dev.watchwolf.serversmanager.server.instantiator;

import dev.watchwolf.core.rpc.stubs.serversmanager.ServerStartedEvent;
import dev.watchwolf.server.ServerStopNotifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

public class Server implements ServerMessageEvent {
    private String ip;
    protected final Collection<ServerStartedEvent> serverStartedListeners;
    protected final Collection<ServerStopNotifier> serverStoppedListeners;
    protected final Collection<ServerMessageEvent> serverMessageListeners;

    public Server(String ip) {
        this.ip = ip;
        this.serverStartedListeners = new ArrayList<>();
        this.serverStoppedListeners = new ArrayList<>();
        this.serverMessageListeners = new ArrayList<>();

        this.subscribeToServerMessageEvents(this);
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

    public void setIp(String ip) {
        this.ip = ip;
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

    @Override
    public void onMessageEvent(String msg) {
        Pattern serverStartedPattern = Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] \\[Server thread/INFO\\]: Done \\([^)]+\\)! For help, type \"help\"");
        if (serverStartedPattern.matcher(msg).find()) {
            try {
                this.raiseServerStartedEvent(); // server started
            } catch (IOException ex) {
                System.err.println(ex.toString());
            }
        }
    }
}
