package dev.watchwolf.serversmanager.server.instantiator;

import dev.watchwolf.core.rpc.stubs.serversmanager.CapturedExceptionEvent;
import dev.watchwolf.core.rpc.stubs.serversmanager.ServerStartedEvent;
import dev.watchwolf.server.ServerStopNotifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

public class ThrowableServer extends Server {
    private final Server wrappedServer;
    private final Collection<CapturedExceptionEvent> capturedExceptionListeners;
    private StringBuilder exception;

    public ThrowableServer(Server s) {
        super(s.getIp());
        this.wrappedServer = s;
        this.capturedExceptionListeners = new ArrayList<>();
        this.exception = null;
    }

    void raiseExceptionEvent(String msg) {
        for (CapturedExceptionEvent e : this.capturedExceptionListeners) {
            try {
                e.capturedException(msg);
            } catch (IOException ignore) {}
        }
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

    @Override
    public void onMessageEvent(String msg) {
        // invoke the original event handler
        super.onMessageEvent(msg);

        if (this.exception == null) {
            // listen for new exceptions
            Pattern startingExceptionPattern = Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] \\[Server thread/ERROR\\]: ");
            if (startingExceptionPattern.matcher(msg).find()) {
                // following there's an exception
                this.exception = new StringBuilder();
            }
        }
        else {
            // did the exception finish?
            Pattern finishingExceptionPattern = Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] \\[Server thread/");
            if (finishingExceptionPattern.matcher(msg).find()) {
                // exception completed; launch event
                this.raiseExceptionEvent(this.exception.toString());
                this.exception = null; // back to listen
            }
            else {
                // keep getting traces
                if (!this.exception.isEmpty()) this.exception.append('\n');
                this.exception.append(msg);
            }
        }
    }
}
