package org.p2p.common;
public class PeerInfo {
    private final String host;
    private final int uploadPort;
    public PeerInfo(String host, int uploadPort) {
        this.host = host;
        this.uploadPort = uploadPort;
    }
    public String getHost() {
        return host;
    }
    public int getUploadPort() {
        return uploadPort;
    }
    @Override
    public String toString() {
        return "PeerInfo{" +
                "host='" + host + '\'' +
                ", uploadPort=" + uploadPort +
                '}';
    }
}
