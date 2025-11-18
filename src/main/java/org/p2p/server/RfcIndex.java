package org.p2p.server;
import org.p2p.common.RfcRecord;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class RfcIndex {
    
    private final Map<Integer, List<RfcRecord>> index = new ConcurrentHashMap<>();
    public synchronized void addRfc(int rfcNumber, String title, String host, int uploadPort) {
        List<RfcRecord> list = index.computeIfAbsent(rfcNumber, k -> new ArrayList<>());
        
        boolean alreadyThere = list.stream()
                .anyMatch(r -> r.getHost().equals(host) && r.getUploadPort() == uploadPort);
        if (!alreadyThere) {
            list.add(new RfcRecord(rfcNumber, title, host, uploadPort));
            System.out.println("[RfcIndex] Added RFC " + rfcNumber + " for host " + host + ":" + uploadPort + " (Total peers with this RFC: " + list.size() + ")");
        } else {
            System.out.println("[RfcIndex] RFC " + rfcNumber + " already registered for host " + host + ":" + uploadPort);
        }
    }
    public synchronized List<RfcRecord> lookup(int rfcNumber) {
        return index.containsKey(rfcNumber)
                ? List.copyOf(index.get(rfcNumber))
                : List.of();
    }
    public synchronized List<RfcRecord> listAll() {
        List<RfcRecord> all = new ArrayList<>();
        for (List<RfcRecord> l : index.values()) {
            all.addAll(l);
        }
        System.out.println("[RfcIndex] LIST ALL request: returning " + all.size() + " RFC entries across " + index.size() + " RFC numbers");
        for (RfcRecord rec : all) {
            System.out.println("[RfcIndex]   RFC " + rec.getRfcNumber() + " at " + rec.getHost() + ":" + rec.getUploadPort());
        }
        return List.copyOf(all);
    }
    
    public synchronized void removeHost(String host) {
        System.out.println("[RfcIndex] WARNING: removeHost(hostname) called - this removes ALL peers with hostname: " + host);
        System.out.println("[RfcIndex] Use removePeer(hostname, port) instead to remove specific peer");
        removePeer(host, -1); 
    }
    public synchronized void removePeer(String host, int uploadPort) {
        if (uploadPort == -1) {
            
            System.out.println("[RfcIndex] Removing all RFCs for hostname: " + host + " (all ports)");
        } else {
            System.out.println("[RfcIndex] Removing all RFCs for peer: " + host + ":" + uploadPort);
        }
        int totalRemoved = 0;
        
        for (Map.Entry<Integer, List<RfcRecord>> entry : index.entrySet()) {
            int rfcNumber = entry.getKey();
            List<RfcRecord> list = entry.getValue();
            int sizeBefore = list.size();
            list.removeIf(r -> {
                boolean matches;
                if (uploadPort == -1) {
                    
                    matches = r.getHost().equals(host);
                } else {
                    
                    matches = r.getHost().equals(host) && r.getUploadPort() == uploadPort;
                }
                if (matches) {
                    System.out.println("[RfcIndex]   Removing RFC " + rfcNumber + " from " + r.getHost() + ":" + r.getUploadPort());
                }
                return matches;
            });
            int sizeAfter = list.size();
            int removed = sizeBefore - sizeAfter;
            totalRemoved += removed;
            if (removed > 0) {
                System.out.println("[RfcIndex]   RFC " + rfcNumber + ": " + removed + " entry(ies) removed, " + sizeAfter + " remaining");
            }
        }
        int emptyLists = (int) index.values().stream().filter(List::isEmpty).count();
        index.values().removeIf(List::isEmpty);
        System.out.println("[RfcIndex] Cleanup complete: " + totalRemoved + " RFC entries removed, " + emptyLists + " empty RFC numbers removed");
    }
}
