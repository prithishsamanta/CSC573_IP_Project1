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
        Thread uploadThread = new Thread(uploadServer, "UploadServer");
        uploadThread.setDaemon(true);
        uploadThread.start();

        // Wait until the server has actually bound to a port
        int boundPort = uploadServer.waitForBoundPort();
        System.out.println("Upload server listening on port " + boundPort);

        // Get local hostname/IP
        String peerHost;
        try {
            peerHost = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            peerHost = "localhost";
        }

        // Initialize P2S and P2P clients
        p2sClient = new P2SClient(
                config.getServerHost(),
                config.getServerPort(),
                peerHost,
                boundPort,
                config.getOsName()
        );
        p2pClient = new P2PClient();

        // Connect to central server
        if (!p2sClient.connect()) {
            System.err.println("Failed to connect to central server. Exiting.");
            System.exit(1);
        }

        // Scan RFC directory and send ADD requests for all RFCs
        scanAndRegisterRfcs();

        // Start CLI menu in main thread
        startCliMenu();

        // Cleanup on exit
        p2sClient.disconnect();
        uploadServer.shutdown();
        System.out.println("Peer shutting down...");
    }

    /**
     * Scan the RFC directory and register all RFCs with the server.
     */
    private static void scanAndRegisterRfcs() {
        File rfcDir = config.getRfcDirectory();
        if (!rfcDir.exists() || !rfcDir.isDirectory()) {
            System.out.println("[PeerMain] RFC directory does not exist: " + rfcDir.getAbsolutePath());
            return;
        }

        File[] files = rfcDir.listFiles((dir, name) -> name.matches("rfc\\d+\\.txt"));
        if (files == null || files.length == 0) {
            System.out.println("[PeerMain] No RFC files found in " + rfcDir.getAbsolutePath());
            return;
        }

        System.out.println("[PeerMain] Found " + files.length + " RFC file(s), registering with server...");

        for (File file : files) {
            String filename = file.getName();
            // Extract RFC number from filename (e.g., "rfc1234.txt" -> 1234)
            try {
                String rfcNumStr = filename.substring(3, filename.length() - 4); // Remove "rfc" prefix and ".txt" suffix
                int rfcNumber = Integer.parseInt(rfcNumStr);
                
                // Extract title from first line of file (or use filename as fallback)
                String title = extractTitleFromFile(file);
                
                if (p2sClient.addRfc(rfcNumber, title)) {
                    System.out.println("[PeerMain] Successfully registered RFC " + rfcNumber + ": " + title);
                } else {
                    System.err.println("[PeerMain] Failed to register RFC " + rfcNumber);
                }
            } catch (NumberFormatException e) {
                System.err.println("[PeerMain] Invalid RFC filename format: " + filename);
            }
        }
    }

    /**
     * Extract title from RFC file (first non-empty line, or filename as fallback).
     */
    private static String extractTitleFromFile(File file) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && line.length() < 200) { // Reasonable title length
                    return line;
                }
            }
        } catch (IOException e) {
            // Fall through to filename
        }
        // Fallback to filename without extension
        String name = file.getName();
        return name.substring(0, name.length() - 4); // Remove ".txt"
    }

    /**
     * Start the interactive CLI menu.
     */
    private static void startCliMenu() {
        Scanner scanner = new Scanner(System.in);
        printMenu();

        while (running && p2sClient.isConnected()) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+");
            String command = parts[0].toUpperCase();

            switch (command) {
                case "LOOKUP":
                    handleLookup(parts);
                    break;
                case "LIST":
                    handleList();
                    break;
                case "GET":
                    handleGet(parts);
                    break;
                case "EXIT":
                case "QUIT":
                    running = false;
                    break;
                case "HELP":
                    printMenu();
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Type HELP for available commands");
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n=== P2P-CI Peer Commands ===");
        System.out.println("LOOKUP RFC <number>  - Find peers that have a specific RFC");
        System.out.println("LIST ALL             - List all RFCs available in the network");
        System.out.println("GET RFC <number>     - Download an RFC from a peer (will lookup first)");
        System.out.println("HELP                 - Show this menu");
        System.out.println("EXIT / QUIT          - Exit the peer");
    }

    private static void handleLookup(String[] parts) {
        if (parts.length < 3 || !parts[1].equalsIgnoreCase("RFC")) {
            System.out.println("Usage: LOOKUP RFC <number>");
            return;
        }

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
