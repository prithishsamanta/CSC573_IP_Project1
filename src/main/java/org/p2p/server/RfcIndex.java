package org.p2p.server;

import org.p2p.common.RfcRecord;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RfcIndex {
    // key: rfcNumber, value: list of RfcRecord (each record = {rfcNumber, title, host, uploadPort})
    private final Map<Integer, List<RfcRecord>> index = new ConcurrentHashMap<>();

    public synchronized void addRfc(int rfcNumber, String title, String host, int uploadPort) {
        List<RfcRecord> list = index.computeIfAbsent(rfcNumber, k -> new ArrayList<>());

        // avoid duplicate host entries for same RFC
        boolean alreadyThere = list.stream()
                .anyMatch(r -> r.getHost().equals(host) && r.getUploadPort() == uploadPort);
        if (!alreadyThere) {
            list.add(new RfcRecord(rfcNumber, title, host, uploadPort));
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
        return List.copyOf(all);
    }

    public synchronized void removeHost(String host) {
        // remove any RfcRecord whose host matches this peer
        for (List<RfcRecord> list : index.values()) {
            list.removeIf(r -> r.getHost().equals(host));
        }
        // Clean up empty RFC lists
        index.values().removeIf(List::isEmpty);
    }
}
