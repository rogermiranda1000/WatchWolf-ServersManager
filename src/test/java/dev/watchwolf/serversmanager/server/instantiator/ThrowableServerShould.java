package dev.watchwolf.serversmanager.server.instantiator;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

// as the wrapper should keep the functionality of a regular Server, we'll inherit the tests
public class ThrowableServerShould extends ServerShould {
    @Override
    protected Server getServer(String ip) {
        return new ThrowableServer(super.getServer(ip));
    }

    @Override
    protected Server getServer() {
        return new ThrowableServer(super.getServer());
    }

    @Test
    void notifyServerStartedEventsDefinedOnWrapper() throws Exception {
        final AtomicBoolean syncronizedObject = new AtomicBoolean(false);

        Server uut = super.getServer();
        assertEquals(Server.class, uut.getClass());
        uut.subscribeToServerStartedEvents(() -> {
            synchronized (syncronizedObject) {
                syncronizedObject.set(true);
                syncronizedObject.notify();
            }
        });

        uut = new ThrowableServer(uut);

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
    void notifyServerStoppedEventsDefinedOnWrapper() throws Exception {
        final AtomicInteger syncronizedObject = new AtomicInteger(0);

        Server uut = super.getServer();
        assertEquals(Server.class, uut.getClass());
        uut.subscribeToServerStoppedEvents(() -> {
            synchronized (syncronizedObject) {
                syncronizedObject.incrementAndGet();
                syncronizedObject.notify();
            }
        });

        uut = new ThrowableServer(uut);

        synchronized (syncronizedObject) {
            // we didn't launch the event, so nothing should invoke
            try {
                syncronizedObject.wait(SMALL_ASSERT_TIMEOUT);
            } catch (InterruptedException ignored) {}
            assertEquals(0, syncronizedObject.get(), "Event was raised before invoking the function");
        }

        uut.raiseServerStoppedEvent();

        synchronized (syncronizedObject) {
            syncronizedObject.wait(WAIT_TIMEOUT);
            assertEquals(1, syncronizedObject.get(), "Event was not risen, or was risen more than once");
        }
    }

    @Test
    void notifyMessageEventsDefinedOnWrapper() throws Exception {
        final AtomicReference<String> syncronizedObject = new AtomicReference<>(null);
        final String sending = "Hello world!";

        Server uut = super.getServer();
        uut.subscribeToServerMessageEvents((msg) -> {
            synchronized (syncronizedObject) {
                syncronizedObject.set(msg);
                syncronizedObject.notify();
            }
        });

        uut = new ThrowableServer(uut);

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
    void notifyExceptionEvents() throws Exception {
        final AtomicReference<String> syncronizedObject = new AtomicReference<>(null);
        final String exception = """
java.lang.IllegalStateException: zip file closed
    at java.util.zip.ZipFile.ensureOpen(ZipFile.java:840) ~[?:?]
    at java.util.zip.ZipFile.getEntry(ZipFile.java:339) ~[?:?]
    at java.util.jar.JarFile.getEntry(JarFile.java:517) ~[?:?]
    at java.util.jar.JarFile.getJarEntry(JarFile.java:472) ~[?:?]
    at org.bukkit.plugin.java.PluginClassLoader.findClass(PluginClassLoader.java:172) ~[spigot-api-1.20.4-R0.1-SNAPSHOT.jar:?]
    at java.lang.ClassLoader.loadClass(ClassLoader.java:592) ~[?:?]
    at org.bukkit.plugin.java.PluginClassLoader.loadClass0(PluginClassLoader.java:117) ~[spigot-api-1.20.4-R0.1-SNAPSHOT.jar:?]
    at org.bukkit.plugin.java.PluginClassLoader.loadClass(PluginClassLoader.java:112) ~[spigot-api-1.20.4-R0.1-SNAPSHOT.jar:?]
    at java.lang.ClassLoader.loadClass(ClassLoader.java:525) ~[?:?]
    at com.rogermiranda1000.mineit.mineable_gems.MinableGems.onEnable(MinableGems.java:83) ~[?:?]
    at org.bukkit.plugin.java.JavaPlugin.setEnabled(JavaPlugin.java:266) ~[spigot-api-1.20.4-R0.1-SNAPSHOT.jar:?]
    at org.bukkit.plugin.java.JavaPluginLoader.enablePlugin(JavaPluginLoader.java:342) ~[spigot-api-1.20.4-R0.1-SNAPSHOT.jar:?]
    at org.bukkit.plugin.SimplePluginManager.enablePlugin(SimplePluginManager.java:480) ~[spigot-api-1.20.4-R0.1-SNAPSHOT.jar:?]
    at org.bukkit.craftbukkit.v1_20_R3.CraftServer.enablePlugin(CraftServer.java:541) ~[spigot-1.20.4-R0.1-SNAPSHOT.jar:4042-Spigot-c198da2-7e43f3b]
    at org.bukkit.craftbukkit.v1_20_R3.CraftServer.enablePlugins(CraftServer.java:455) ~[spigot-1.20.4-R0.1-SNAPSHOT.jar:4042-Spigot-c198da2-7e43f3b]
    at net.minecraft.server.MinecraftServer.loadWorld0(MinecraftServer.java:623) ~[spigot-1.20.4-R0.1-SNAPSHOT.jar:4042-Spigot-c198da2-7e43f3b]
    at net.minecraft.server.MinecraftServer.loadLevel(MinecraftServer.java:409) ~[spigot-1.20.4-R0.1-SNAPSHOT.jar:4042-Spigot-c198da2-7e43f3b]
    at net.minecraft.server.dedicated.DedicatedServer.e(DedicatedServer.java:250) ~[spigot-1.20.4-R0.1-SNAPSHOT.jar:4042-Spigot-c198da2-7e43f3b]
    at net.minecraft.server.MinecraftServer.w(MinecraftServer.java:1000) ~[spigot-1.20.4-R0.1-SNAPSHOT.jar:4042-Spigot-c198da2-7e43f3b]
    at net.minecraft.server.MinecraftServer.lambda$0(MinecraftServer.java:304) ~[spigot-1.20.4-R0.1-SNAPSHOT.jar:4042-Spigot-c198da2-7e43f3b]
    at java.lang.Thread.run(Thread.java:840) ~[?:?]""";
        final String log = """
[08:48:35] [Server thread/INFO]: [MineableGems] Enabling MineableGems v1.11.3
[08:48:36] [Server thread/INFO]: [MineIt-MineableGems] Enabling MineIt-MineableGems v1.1
[08:48:36] [Server thread/WARN]: java.lang.IllegalStateException: zip file closed
[08:48:36] [Server thread/WARN]: 	at java.base/java.util.zip.ZipFile.ensureOpen(ZipFile.java:840)
[08:48:36] [Server thread/WARN]: 	at java.base/java.util.zip.ZipFile.getEntry(ZipFile.java:339)
[08:48:36] [Server thread/WARN]: 	at java.base/java.util.jar.JarFile.getEntry(JarFile.java:517)
[08:48:36] [Server thread/WARN]: 	at java.base/java.util.jar.JarFile.getJarEntry(JarFile.java:472)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.PluginClassLoader.findClass(PluginClassLoader.java:172)
[08:48:36] [Server thread/WARN]: 	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:592)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.PluginClassLoader.loadClass0(PluginClassLoader.java:117)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.PluginClassLoader.loadClass(PluginClassLoader.java:112)
[08:48:36] [Server thread/WARN]: 	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:525)
[08:48:36] [Server thread/WARN]: 	at com.rogermiranda1000.helper.RogerPlugin.onEnable(RogerPlugin.java:313)
[08:48:36] [Server thread/WARN]: 	at com.rogermiranda1000.mineit.mineable_gems.MinableGems.onEnable(MinableGems.java:80)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.JavaPlugin.setEnabled(JavaPlugin.java:266)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.JavaPluginLoader.enablePlugin(JavaPluginLoader.java:342)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.SimplePluginManager.enablePlugin(SimplePluginManager.java:480)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.craftbukkit.v1_20_R3.CraftServer.enablePlugin(CraftServer.java:541)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.craftbukkit.v1_20_R3.CraftServer.enablePlugins(CraftServer.java:455)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.MinecraftServer.loadWorld0(MinecraftServer.java:623)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.MinecraftServer.loadLevel(MinecraftServer.java:409)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.dedicated.DedicatedServer.e(DedicatedServer.java:250)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.MinecraftServer.w(MinecraftServer.java:1000)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.MinecraftServer.lambda$0(MinecraftServer.java:304)
[08:48:36] [Server thread/WARN]: 	at java.base/java.lang.Thread.run(Thread.java:840)
[08:48:36] [Server thread/WARN]: java.lang.IllegalStateException: zip file closed
[08:48:36] [Server thread/WARN]: 	at java.base/java.util.zip.ZipFile.ensureOpen(ZipFile.java:840)
[08:48:36] [Server thread/WARN]: 	at java.base/java.util.zip.ZipFile.getEntry(ZipFile.java:339)
[08:48:36] [Server thread/WARN]: 	at java.base/java.util.jar.JarFile.getEntry(JarFile.java:517)
[08:48:36] [Server thread/WARN]: 	at java.base/java.util.jar.JarFile.getJarEntry(JarFile.java:472)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.PluginClassLoader.findClass(PluginClassLoader.java:172)
[08:48:36] [Server thread/WARN]: 	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:592)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.PluginClassLoader.loadClass0(PluginClassLoader.java:117)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.PluginClassLoader.loadClass(PluginClassLoader.java:112)
[08:48:36] [Server thread/WARN]: 	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:525)
[08:48:36] [Server thread/WARN]: 	at com.rogermiranda1000.helper.RogerPlugin.onEnable(RogerPlugin.java:334)
[08:48:36] [Server thread/WARN]: 	at com.rogermiranda1000.mineit.mineable_gems.MinableGems.onEnable(MinableGems.java:80)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.JavaPlugin.setEnabled(JavaPlugin.java:266)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.java.JavaPluginLoader.enablePlugin(JavaPluginLoader.java:342)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.plugin.SimplePluginManager.enablePlugin(SimplePluginManager.java:480)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.craftbukkit.v1_20_R3.CraftServer.enablePlugin(CraftServer.java:541)
[08:48:36] [Server thread/WARN]: 	at org.bukkit.craftbukkit.v1_20_R3.CraftServer.enablePlugins(CraftServer.java:455)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.MinecraftServer.loadWorld0(MinecraftServer.java:623)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.MinecraftServer.loadLevel(MinecraftServer.java:409)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.dedicated.DedicatedServer.e(DedicatedServer.java:250)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.MinecraftServer.w(MinecraftServer.java:1000)
[08:48:36] [Server thread/WARN]: 	at net.minecraft.server.MinecraftServer.lambda$0(MinecraftServer.java:304)
[08:48:36] [Server thread/WARN]: 	at java.base/java.lang.Thread.run(Thread.java:840)
[08:48:36] [Server thread/INFO]: [MineIt-MineableGems] Disabling MineIt-MineableGems v1.1
[08:48:36] [Server thread/ERROR]: Error occurred while enabling MineIt-MineableGems v1.1 (Is it up to date?)
""" + exception + "\n[08:48:36] [Server thread/INFO]: Done (21.650s)! For help, type \"help\"";

        ThrowableServer uut = (ThrowableServer) getServer();
        uut.subscribeToExceptionEvents((msg) -> {
            synchronized (syncronizedObject) {
                syncronizedObject.set(msg);
                syncronizedObject.notify();
            }
        });

        // exceptions are processed through the regular messages
        for (String logLine : log.split("\n")) {
            uut.raiseServerMessageEvent(logLine); // we'll get the messagess one by one
        }

        synchronized (syncronizedObject) {
            syncronizedObject.wait(WAIT_TIMEOUT);
            assertEquals(exception, syncronizedObject.get(), "Strings don't match:\n  Expecting:\n" + exception + "\n  Got:\n" + syncronizedObject.get());
        }
    }

    @Test
    void detectServerStartedByMessageDefinedOnWrapper() throws Exception {
        final AtomicInteger syncronizedObject = new AtomicInteger(0);

        Server uut = super.getServer();
        assertEquals(Server.class, uut.getClass());
        uut.subscribeToServerStartedEvents(() -> {
            synchronized (syncronizedObject) {
                syncronizedObject.incrementAndGet();
                syncronizedObject.notify();
            }
        });

        uut = new ThrowableServer(uut);

        final String startupSequence = """
[18:47:41] [Server thread/INFO]: Preparing level "world"
[18:47:44] [Server thread/INFO]: Preparing start region for dimension minecraft:overworld
[18:47:44] [Server thread/INFO]: Time elapsed: 118 ms
[18:47:44] [Server thread/INFO]: Preparing start region for dimension minecraft:the_nether
[18:47:44] [Server thread/INFO]: Time elapsed: 101 ms
[18:47:44] [Server thread/INFO]: Preparing start region for dimension minecraft:the_end
[18:47:44] [Server thread/INFO]: Time elapsed: 81 ms
[18:47:45] [Server thread/INFO]: [MineIt] WorldGuard plugin detected.
[18:47:45] [Server thread/INFO]: Running delayed init tasks
"""; // next we get the 'Done' message

        for (String line : startupSequence.split("\n")) uut.raiseServerMessageEvent(line);

        synchronized (syncronizedObject) {
            // we didn't launch the event, so nothing should invoke
            try {
                syncronizedObject.wait(SMALL_ASSERT_TIMEOUT);
            } catch (InterruptedException ignored) {}
            assertEquals(0, syncronizedObject.get(), "Event was raised before invoking the function");
        }

        uut.raiseServerMessageEvent("[18:47:45] [Server thread/INFO]: Done (6.656s)! For help, type \"help\"");

        synchronized (syncronizedObject) {
            syncronizedObject.wait(WAIT_TIMEOUT);
            assertEquals(1, syncronizedObject.get(), "Event was " + ((syncronizedObject.get() == 0) ? "not risen" : "risen more than once"));
        }
    }

    @Test
    void detectServerStartedByMessageCalledOnWrapper() throws Exception {
        final AtomicInteger syncronizedObject = new AtomicInteger(0);

        Server wrapper = super.getServer();
        assertEquals(Server.class, wrapper.getClass());

        ThrowableServer uut = new ThrowableServer(wrapper);
        uut.subscribeToServerStartedEvents(() -> {
            synchronized (syncronizedObject) {
                syncronizedObject.incrementAndGet();
                syncronizedObject.notify();
            }
        });

        final String startupSequence = """
[18:47:41] [Server thread/INFO]: Preparing level "world"
[18:47:44] [Server thread/INFO]: Preparing start region for dimension minecraft:overworld
[18:47:44] [Server thread/INFO]: Time elapsed: 118 ms
[18:47:44] [Server thread/INFO]: Preparing start region for dimension minecraft:the_nether
[18:47:44] [Server thread/INFO]: Time elapsed: 101 ms
[18:47:44] [Server thread/INFO]: Preparing start region for dimension minecraft:the_end
[18:47:44] [Server thread/INFO]: Time elapsed: 81 ms
[18:47:45] [Server thread/INFO]: [MineIt] WorldGuard plugin detected.
[18:47:45] [Server thread/INFO]: Running delayed init tasks
"""; // next we get the 'Done' message

        for (String line : startupSequence.split("\n")) wrapper.raiseServerMessageEvent(line);

        synchronized (syncronizedObject) {
            // we didn't launch the event, so nothing should invoke
            try {
                syncronizedObject.wait(SMALL_ASSERT_TIMEOUT);
            } catch (InterruptedException ignored) {}
            assertEquals(0, syncronizedObject.get(), "Event was raised before invoking the function");
        }

        wrapper.raiseServerMessageEvent("[18:47:45] [Server thread/INFO]: Done (6.656s)! For help, type \"help\"");

        synchronized (syncronizedObject) {
            syncronizedObject.wait(WAIT_TIMEOUT);
            assertEquals(1, syncronizedObject.get(), "Event was " + ((syncronizedObject.get() == 0) ? "not risen" : "risen more than once"));
        }
    }
}
