package org.p2p.peer;

import org.p2p.common.PeerInfo;
import org.p2p.common.RfcRecord;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point for the peer process.
 *
 * Responsibilities:
 *  - Parse CLI args
 *  - Start the UploadServer on a given or random port
 *  - Connect to central server and maintain control connection
 *  - Send ADD requests for all local RFCs on startup
 *  - Provide CLI menu for LOOKUP, LIST, GET commands
 */
public class PeerMain {

    private static P2SClient p2sClient;
    private static P2PClient p2pClient;
    private static PeerConfig config;
    private static UploadServer uploadServer;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        config = PeerConfig.fromArgs(args);

        System.out.println("Starting peer with config:");
        System.out.println("  Server host   : " + config.getServerHost());
        System.out.println("  Server port   : " + config.getServerPort());
        System.out.println("  RFC directory : " + config.getRfcDirectory().getAbsolutePath());
        System.out.println("  Upload port   : " + (config.getUploadPort() == 0 ? "auto (ephemeral)" : config.getUploadPort()));
        System.out.println("  OS            : " + config.getOsName());

        // Start the upload server in its own thread
        uploadServer = new UploadServer(config.getUploadPort(), config.getRfcDirectory(), config.getOsName());
        UploadServer uploadServer = new UploadServer(config.getUploadPort(), config.getRfcDirectory(), config.getOsName());
        Thread uploadThread = new Thread(uploadServer, "UploadServer");
        uploadThread.setDaemon(true);
        uploadThread.start();

        int boundPort = uploadServer.waitForBoundPort();
        System.out.println("Upload server listening on port " + boundPort);
        try {
            int rfcNumber = Integer.parseInt(parts[2]);
            List<RfcRecord> records = p2sClient.lookupRfc(rfcNumber);

            if (records.isEmpty()) {
                System.out.println("No peers found with RFC " + rfcNumber);
            } else {
                System.out.println("Found " + records.size() + " peer(s) with RFC " + rfcNumber + ":");
                for (RfcRecord record : records) {
                    System.out.println("  RFC " + record.getRfcNumber() + " - " + record.getTitle() +
                            " @ " + record.getHost() + ":" + record.getUploadPort());
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid RFC number: " + parts[2]);
        }
    }

    private static void handleList() {
        List<RfcRecord> records = p2sClient.listAll();

        if (records.isEmpty()) {
            System.out.println("No RFCs found in the network");
        } else {
            System.out.println("All RFCs in network (" + records.size() + " total):");
            for (RfcRecord record : records) {
                System.out.println("  RFC " + record.getRfcNumber() + " - " + record.getTitle() +
                        " @ " + record.getHost() + ":" + record.getUploadPort());
            }
        }
    }

    private static void handleGet(String[] parts) {
        if (parts.length < 3 || !parts[1].equalsIgnoreCase("RFC")) {
            System.out.println("Usage: GET RFC <number>");
            return;
        }

        try {
            int rfcNumber = Integer.parseInt(parts[2]);

            // First, lookup to find peers that have this RFC
            System.out.println("Looking up RFC " + rfcNumber + "...");
            List<RfcRecord> records = p2sClient.lookupRfc(rfcNumber);

            if (records.isEmpty()) {
                System.out.println("No peers found with RFC " + rfcNumber);
                return;
            }

            // Use the first available peer
            RfcRecord record = records.get(0);
            System.out.println("Downloading RFC " + rfcNumber + " from " +
                    record.getHost() + ":" + record.getUploadPort() + "...");

            PeerInfo peer = new PeerInfo(record.getHost(), record.getUploadPort());
            boolean success = p2pClient.downloadRfc(peer, rfcNumber, config.getRfcDirectory(), config.getOsName());

            if (success) {
                System.out.println("Successfully downloaded RFC " + rfcNumber);
                
                // Register the newly downloaded RFC with the server
                String title = record.getTitle();
                if (p2sClient.addRfc(rfcNumber, title)) {
                    System.out.println("Registered RFC " + rfcNumber + " with server");
                }
            } else {
                System.err.println("Failed to download RFC " + rfcNumber);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid RFC number: " + parts[2]);
        }
    }
}
