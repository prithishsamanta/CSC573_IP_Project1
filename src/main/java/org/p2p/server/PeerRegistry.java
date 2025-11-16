package org.p2p.server;
import org.p2p.common.PeerInfo;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class PeerRegistry {
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();
    public void addPeer(String host, int uploadPort) {
        peers.put(host, new PeerInfo(host, uploadPort));
    }
    public PeerInfo getPeer(String host) {
        return peers.get(host);
    }
    public void removePeer(String host) {
        peers.remove(host);
    }
    public Map<String, PeerInfo> snapshot() {
        return Map.copyOf(peers);
    }
}
