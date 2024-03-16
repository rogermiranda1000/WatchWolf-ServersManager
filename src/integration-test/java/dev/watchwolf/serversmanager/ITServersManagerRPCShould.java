package dev.watchwolf.serversmanager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10*60)
public class ITServersManagerRPCShould {
    private static Logger LOGGER = LogManager.getLogger(ITServersManagerRPCShould.class.getName());

    public static byte []getStartServerSequence() {
        return new byte[]{
                // target: servers manager ; return: no ; operation: start server
                0b0001_0_000, 0b00000000,
                // server type (String; "Spigot")
                0x00, 0x06, // 6 character-long string
                'S', 'p', 'i', 'g', 'o', 't',
                // server version (String; "1.19")
                0x00, 0x04, // 4 character-long string
                '1', '.', '1', '9',
                // plugins, maps & config files (arrays; all empty)
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00
        };
    }

    public static String getString(BufferedReader in, int lenght) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < lenght; n++) sb.append((char)in.read());
        return sb.toString();
    }

    public static Thread getJarMainRunnable() {
        return new Thread(() -> {
            try {
                ServersManager.main(new String[]{});
            } catch (Exception ex) {
                LOGGER.error("Exception while running main thread", ex);
            }
        });
    }

    public static Socket waitUntilReadyAndConnect(String host, int port) throws TimeoutException {
        int tries = 10;
        while(tries > 0) {
            try {
                return new Socket(host, port);
            } catch(IOException ex) {
                LOGGER.debug("Couldn't connect to " + host + ":" + port + "; trying again after 2s...");
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException ignore) {}
                tries--;
            }
        }
        throw new TimeoutException("Exceeded max retries to connect to " + host + ":" + port);
    }

    @Test
    public void triggerAServerStartAfterStartServerRPCSequence() throws Exception {
        // launch ServersManager.main in separate thread
        Thread mainThread = getJarMainRunnable();
        mainThread.start();
        LOGGER.info("Main thread launched in parallel.");

        try(Socket clientSocket = waitUntilReadyAndConnect("127.0.0.1", 8000);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            // send hardcoded "Start Server" sequence
            LOGGER.info("Sending 'start server' request...");
            clientSocket.getOutputStream().write(getStartServerSequence());

            // we expect to get the IP
            // from: servers manager ; return: yes ; operation: start server
            int startPacket = in.read();
            assertEquals(0b0001_1_000, startPacket, "We were expecting a return from servers manager of the 'start server' request; got " + System.out.format("%08d%n", startPacket) + " instead");
            assertEquals(0b00000000, in.read());
            int ipLength = in.read() | (in.read()<<8);
            LOGGER.debug("IP size: " + ipLength);
            assertTrue(ipLength > 0, "Empty (or invalid array) got");
            String ip = getString(in, ipLength);
            LOGGER.debug("IP got: " + ip);
            assertEquals(8001, Integer.valueOf(ip.split(":")[1]));
        }

        LOGGER.info("Waiting for main thread to exit...");
        mainThread.join(2*60_000);
    }

    @Test
    public void supportMultipleSessions() throws Exception {

    }
}
