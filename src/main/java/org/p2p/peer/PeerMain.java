package org.p2p.peer;

import org.p2p.common.PeerInfo;
import org.p2p.common.RfcRecord;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

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

        uploadServer = new UploadServer(config.getUploadPort(), config.getRfcDirectory(), config.getOsName());
        Thread uploadThread = new Thread(uploadServer, "UploadServer");
        uploadThread.setDaemon(true);
        uploadThread.start();

        int boundPort = uploadServer.waitForBoundPort();
        System.out.println("Upload server listening on port " + boundPort);

        String peerHost = getPeerHostname();
        System.out.println("Peer hostname: " + peerHost);

        p2sClient = new P2SClient(config.getServerHost(), config.getServerPort(), 
                                   peerHost, boundPort, config.getOsName());
        p2pClient = new P2PClient();

        if (!p2sClient.connect()) {
            System.err.println("Failed to connect to server. Exiting.");
            return;
        }

        scanAndRegisterRfcs();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down peer...");
            running = false;
            if (p2sClient != null) {
                p2sClient.disconnect();
            }
        }));

        System.out.println("\n=== P2P-CI Peer Ready ===");
        System.out.println("Commands:");
        System.out.println("  ADD RFC <num> <title>  - Register an RFC with the server");
        System.out.println("  LIST ALL               - List all RFCs in the network");
        System.out.println("  LOOKUP RFC <num>       - Find peers with a specific RFC");
        System.out.println("  GET RFC <num>          - Download an RFC from a peer");
        System.out.println("  EXIT                   - Exit the peer\n");

        Scanner scanner = new Scanner(System.in);
        while (running) {
            try {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                String command = parts[0].toUpperCase();

                switch (command) {
                    case "ADD":
                        handleAdd(parts);
                        break;
                    case "LIST":
                        handleList();
                        break;
                    case "LOOKUP":
                        handleLookup(parts);
                        break;
                    case "GET":
                        handleGet(parts);
                        break;
                    case "EXIT":
                    case "QUIT":
                        running = false;
                        break;
                    default:
                        System.out.println("Unknown command: " + command);
                        System.out.println("Type ADD, LIST, LOOKUP, GET, or EXIT");
                }
            } catch (Exception e) {
                System.err.println("Error processing command: " + e.getMessage());
            }
        }

        scanner.close();
        p2sClient.disconnect();
        uploadServer.shutdown();
        System.out.println("Peer shutdown complete.");
    }

    private static String getPeerHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.err.println("Warning: Could not determine hostname, using 'localhost'");
            return "localhost";
        }
    }

    private static void scanAndRegisterRfcs() {
        File rfcDir = config.getRfcDirectory();
        if (!rfcDir.exists() || !rfcDir.isDirectory()) {
            System.out.println("RFC directory does not exist or is not a directory: " + rfcDir);
            return;
        }

        File[] rfcFiles = rfcDir.listFiles((dir, name) -> 
            name.toLowerCase().startsWith("rfc") && name.toLowerCase().endsWith(".txt"));

        if (rfcFiles == null || rfcFiles.length == 0) {
            System.out.println("No RFC files found in directory. Peer has no RFCs to share.");
            return;
        }

        System.out.println("\nRegistering " + rfcFiles.length + " RFC(s) with server...");
        int successCount = 0;

        for (File rfcFile : rfcFiles) {
            try {
                String filename = rfcFile.getName();
                String numStr = filename.substring(3, filename.length() - 4);
                int rfcNumber = Integer.parseInt(numStr);
                
                String title = "RFC " + rfcNumber;
                
                if (p2sClient.addRfc(rfcNumber, title)) {
                    successCount++;
                    System.out.println("  Registered RFC " + rfcNumber);
                } else {
                    System.err.println("  Failed to register RFC " + rfcNumber);
                }
            } catch (NumberFormatException e) {
                System.err.println("  Skipping file with invalid RFC number: " + rfcFile.getName());
            }
        }

        System.out.println("Successfully registered " + successCount + " out of " + rfcFiles.length + " RFCs\n");
    }

    private static void handleAdd(String[] parts) {
        // Usage: ADD RFC <number> <title...>
        if (parts.length < 4 || !parts[1].equalsIgnoreCase("RFC")) {
            System.out.println("Usage: ADD RFC <number> <title>");
            System.out.println("Example: ADD RFC 1234 Introduction to TCP/IP");
            return;
        }

        try {
            int rfcNumber = Integer.parseInt(parts[2]);
            
            // Rest of the parts are the title
            StringBuilder titleBuilder = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (i > 3) titleBuilder.append(" ");
                titleBuilder.append(parts[i]);
            }
            String title = titleBuilder.toString();
            
            if (title.isEmpty()) {
                System.out.println("Error: Title cannot be empty");
                return;
            }
            
            // Create the RFC file in the local directory with title in filename
            File rfcDir = config.getRfcDirectory();
            // Sanitize title for filename: replace spaces with underscores, remove special chars
            String sanitizedTitle = title.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "_");
            String filename = sanitizedTitle + ".txt";
            File rfcFile = new File(rfcDir, filename);
            
            try (FileWriter writer = new FileWriter(rfcFile)) {
                writer.write("RFC " + rfcNumber + " - " + title + "\n");
                writer.write("This RFC file was created via ADD command.\n");
            } catch (IOException e) {
                System.err.println("Error creating RFC file: " + e.getMessage());
                return;
            }
            
            System.out.println("Created local file: " + rfcFile.getAbsolutePath());
            System.out.println("Adding RFC " + rfcNumber + " with title: " + title);
            
            if (p2sClient.addRfc(rfcNumber, title)) {
                System.out.println("Successfully registered RFC " + rfcNumber + " with server");
            } else {
                System.err.println("Failed to register RFC " + rfcNumber + " with server");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid RFC number: " + parts[2]);
        }
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
                    System.out.println("  RFC " + record.getRfcNumber() + " " + record.getTitle() +
                            " " + record.getHost() + " " + record.getUploadPort());
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
                // Format: hostname port number_title
                String title = record.getTitle();
                // Remove "RFC " prefix from title if it exists to avoid duplication
                if (title.startsWith("RFC ")) {
                    title = title.substring(4);
                }
                System.out.println("  " + record.getHost() + " " + record.getUploadPort() + 
                        " " + record.getRfcNumber() + "_" + title);
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

            System.out.println("Looking up RFC " + rfcNumber + "...");
            List<RfcRecord> records = p2sClient.lookupRfc(rfcNumber);

            if (records.isEmpty()) {
                System.out.println("No peers found with RFC " + rfcNumber);
                return;
            }

            RfcRecord record = records.get(0);
            System.out.println("Downloading RFC " + rfcNumber + " from " +
                    record.getHost() + ":" + record.getUploadPort() + "...");

            PeerInfo peer = new PeerInfo(record.getHost(), record.getUploadPort());
            boolean success = p2pClient.downloadRfc(peer, rfcNumber, config.getRfcDirectory(), config.getOsName());

            if (success) {
                System.out.println("Successfully downloaded RFC " + rfcNumber);
                
                String title = record.getTitle();
                if (p2sClient.addRfc(rfcNumber, title)) {
                    System.out.println("Registered RFC " + rfcNumber + " with server");
                } else {
                    System.err.println("Warning: Failed to register RFC " + rfcNumber + " with server");
                }
            } else {
                System.err.println("Failed to download RFC " + rfcNumber);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid RFC number: " + parts[2]);
        }
    }
}
