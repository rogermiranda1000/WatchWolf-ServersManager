package dev.watchwolf.serversmanager.server.instantiator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ServerShould {
    protected static final int WAIT_TIMEOUT = 60,
                            SMALL_ASSERT_TIMEOUT = 20;

    protected Server getServer(String ip) {
        return new Server(ip);
    }

    protected Server getServer() {
        return this.getServer("127.0.0.1:25565");
    }

    @Test
    void reportItsIp() {
        String ip = "127.0.0.1:25555";

        Server uut = getServer(ip);

        assertEquals(ip, uut.getIp());
    }

    @Test
    void notifyServerStartedEvent() throws Exception {
        final AtomicBoolean syncronizedObject = new AtomicBoolean(false);

        Server uut = getServer();
        uut.subscribeToServerStartedEvents(() -> {
            synchronized (syncronizedObject) {
                syncronizedObject.set(true);
                syncronizedObject.notify();
            }
        });

        synchronized (syncronizedObject) {
            // we didn't launch the event, so nothing should invoke
            try {
                syncronizedObject.wait(SMALL_ASSERT_TIMEOUT);
            } catch (InterruptedException ignored) {}
            assertFalse(syncronizedObject.get(), "Event was raised before invoking the function");
        }

        uut.raiseServerStartedEvent();

        synchronized (syncronizedObject) {
            syncronizedObject.wait(WAIT_TIMEOUT);
            assertTrue(syncronizedObject.get(), "Event was not risen");
        }
    }

    @Test
    void notifyServerStoppedEvent() throws Exception {
        final AtomicBoolean syncronizedObject = new AtomicBoolean(false);

        Server uut = getServer();
        uut.subscribeToServerStoppedEvents(() -> {
            synchronized (syncronizedObject) {
                syncronizedObject.set(true);
                syncronizedObject.notify();
            }
        });

        synchronized (syncronizedObject) {
            // we didn't launch the event, so nothing should invoke
            try {
                syncronizedObject.wait(SMALL_ASSERT_TIMEOUT);
            } catch (InterruptedException ignored) {}
            assertFalse(syncronizedObject.get(), "Event was raised before invoking the function");
        }

        uut.raiseServerStoppedEvent();

        synchronized (syncronizedObject) {
            syncronizedObject.wait(WAIT_TIMEOUT);
            assertTrue(syncronizedObject.get(), "Event was not risen");
        }
    }

    @Test
    void notifyMessageEvent() throws Exception {
        final AtomicReference<String> syncronizedObject = new AtomicReference<>(null);
        final String sending = "Hello world!";

        Server uut = getServer();
        uut.subscribeToServerMessageEvents((msg) -> {
            synchronized (syncronizedObject) {
                syncronizedObject.set(msg);
                syncronizedObject.notify();
            }
        });

        synchronized (syncronizedObject) {
            // we didn't launch the event, so nothing should invoke
            try {
                syncronizedObject.wait(SMALL_ASSERT_TIMEOUT);
            } catch (InterruptedException ignored) {}
            assertNull(syncronizedObject.get(), "Event was raised before invoking the function");
        }

        uut.raiseServerMessageEvent(sending);

        synchronized (syncronizedObject) {
            syncronizedObject.wait(WAIT_TIMEOUT);
            assertEquals(sending, syncronizedObject.get(), "Event was not risen");
        }
    }

    @Test
    void notifyMultipleMessagesEvent() throws Exception {
        final ArrayList<String> syncronizedObject = new ArrayList<>();
        final String []sending = new String[]{"Hello world!", "This is a test"};

        Server uut = getServer();
        uut.subscribeToServerMessageEvents((msg) -> {
            synchronized (syncronizedObject) {
                syncronizedObject.add(msg);
                syncronizedObject.notify();
            }
        });

        synchronized (syncronizedObject) {
            // we didn't launch the event, so nothing should invoke
            try {
                syncronizedObject.wait(SMALL_ASSERT_TIMEOUT);
            } catch (InterruptedException ignored) {}
            assertEquals(0, syncronizedObject.size(), "Event was raised before invoking the function");
        }

        for (int index = 0; index < sending.length; index++) {
            uut.raiseServerMessageEvent(sending[index]);
        }

        synchronized (syncronizedObject) {
            syncronizedObject.wait(WAIT_TIMEOUT);
            assertEquals(sending.length, syncronizedObject.size(), "Event was not risen");
            for (int index = 0; index < sending.length; index++) {
                assertEquals(sending[index], syncronizedObject.get(index));
            }
        }
    }
}
