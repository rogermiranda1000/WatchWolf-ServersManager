package dev.watchwolf.serversmanager.server.instantiator;

import dev.watchwolf.core.rpc.stubs.serversmanager.CapturedExceptionEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServerStartedEvent;
import dev.watchwolf.server.ServerStopNotifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ThrowableServer extends Server {
    private final Server wrappedServer;
    private final Collection<CapturedExceptionEvent> capturedExceptionListeners;

    public ThrowableServer(Server s) {
        super(s.getIp());
        this.wrappedServer = s;
        this.capturedExceptionListeners = new ArrayList<>();
    }

    void raiseExceptionEvent(String msg) throws IOException {
        for (CapturedExceptionEvent e : this.capturedExceptionListeners) e.capturedException(msg);
    }

    @Override
    void raiseServerStartedEvent() throws IOException {
        this.wrappedServer.raiseServerStartedEvent();
        for (ServerStartedEvent e : this.serverStartedListeners) e.serverStarted();
    }

    @Override
    void raiseServerStoppedEvent() {
        this.wrappedServer.raiseServerStoppedEvent();
        for (ServerStopNotifier e : this.serverStoppedListeners) e.onServerStop();
    }

    @Override
    void raiseServerMessageEvent(String msg) {
        this.wrappedServer.raiseServerMessageEvent(msg);
        for (ServerMessageEvent e : this.serverMessageListeners) e.onMessageEvent(msg);
    }

    public Server subscribeToExceptionEvents(CapturedExceptionEvent subscriber) {
        this.capturedExceptionListeners.add(subscriber);
        return this;
    }
}
