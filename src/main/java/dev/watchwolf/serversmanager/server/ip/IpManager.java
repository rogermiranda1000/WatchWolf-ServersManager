package dev.watchwolf.serversmanager.server.ip;

public interface IpManager {
    /**
     * Docker behaves differently depending
     * @param originalIp
     * @param callerIp
     * @return
     */
    String getIp(String originalIp, String callerIp);
}
