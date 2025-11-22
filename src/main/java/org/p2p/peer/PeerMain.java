package org.p2p.peer;
import org.p2p.common.PeerInfo;
import org.p2p.common.RfcRecord;
import java.io.File;
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
        System.out.println("  ADD RFC <num> P2P-CI/1.0       - Register an RFC with the server (will prompt for Host, Port, Title)");
        System.out.println("  LIST ALL P2P-CI/1.0            - List all RFCs in the network (will prompt for Host, Port)");
        System.out.println("  LOOKUP RFC <num> P2P-CI/1.0    - Find peers with a specific RFC (will prompt for Host, Port, Title)");
        System.out.println("  GET RFC <num> P2P-CI/1.0       - Download an RFC from a peer (will prompt for Host, OS)");
        System.out.println("  EXIT                           - Exit the peer\n");
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
                        handleAdd(parts, scanner);
                        break;
                    case "LIST":
                        handleList(parts, scanner);
                        break;
                    case "LOOKUP":
                        handleLookup(parts, scanner);
                        break;
                    case "GET":
                        handleGet(parts, scanner);
                        break;
                    case "EXIT":
                        handleExit();
                        break;
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

    private static File findRfcFileInDirectory(File directory, int rfcNumber) {
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }
        File[] files = directory.listFiles((dir, name) -> 
            name.toUpperCase().startsWith("RFC_") && name.toLowerCase().endsWith(".txt"));
        
        if (files != null) {
            for (File file : files) {
                try {
                    String filename = file.getName();
                    String withoutPrefix = filename.substring(4);
                    String withoutSuffix = withoutPrefix.substring(0, withoutPrefix.length() - 4);
                    int firstUnderscore = withoutSuffix.indexOf('_');
                    if (firstUnderscore != -1) {
                        String numStr = withoutSuffix.substring(0, firstUnderscore);
                        int fileRfcNumber = Integer.parseInt(numStr);
                        if (fileRfcNumber == rfcNumber) {
                            return file;
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return null;
    }
    private static void scanAndRegisterRfcs() {
        File rfcDir = config.getRfcDirectory();
        if (!rfcDir.exists() || !rfcDir.isDirectory()) {
            System.out.println("RFC directory does not exist or is not a directory: " + rfcDir);
            return;
        }
        
        File[] rfcFiles = rfcDir.listFiles((dir, name) -> 
            name.toUpperCase().startsWith("RFC_") && name.toLowerCase().endsWith(".txt"));
        if (rfcFiles == null || rfcFiles.length == 0) {
            System.out.println("No RFC files found in directory. Peer has no RFCs to share.");
            return;
        }
        System.out.println("\nRegistering " + rfcFiles.length + " RFC(s) with server...");
        int successCount = 0;
        for (File rfcFile : rfcFiles) {
            try {
                String filename = rfcFile.getName();
                
                String withoutPrefix = filename.substring(4); 
                String withoutSuffix = withoutPrefix.substring(0, withoutPrefix.length() - 4); 
                
                int firstUnderscore = withoutSuffix.indexOf('_');
                if (firstUnderscore == -1) {
                    System.err.println("  Skipping file with invalid format (missing title): " + rfcFile.getName());
                    continue;
                }
                String numStr = withoutSuffix.substring(0, firstUnderscore);
                String title = withoutSuffix.substring(firstUnderscore + 1);
                
                title = title.replace('_', ' ');
                int rfcNumber = Integer.parseInt(numStr);
                if (p2sClient.addRfc(rfcNumber, title, "P2P-CI/1.0")) {
                    successCount++;
                    System.out.println("  Registered RFC " + rfcNumber + ": " + title);
                } else {
                    System.err.println("  Failed to register RFC " + rfcNumber);
                }
            } catch (NumberFormatException e) {
                System.err.println("  Skipping file with invalid RFC number: " + rfcFile.getName());
            } catch (Exception e) {
                System.err.println("  Skipping file with invalid format: " + rfcFile.getName() + " - " + e.getMessage());
            }
        }
        System.out.println("Successfully registered " + successCount + " out of " + rfcFiles.length + " RFCs\n");
    }
    private static void handleAdd(String[] parts, Scanner scanner) {
        if (parts.length < 4 || !parts[1].equalsIgnoreCase("RFC")) {
            System.out.println("Usage: ADD RFC <number> P2P-CI/1.0");
            System.out.println("Example: ADD RFC 123 P2P-CI/1.0");
            System.out.println("You will be prompted for Host, Port, and Title");
            return;
        }
        try {
            int rfcNumber = Integer.parseInt(parts[2]);
            String version = parts[3];
            
            System.out.print("Host: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String host = scanner.nextLine().trim();
            if (host.isEmpty()) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: Host cannot be empty");
                return;
            }
            
            System.out.print("Port: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String portStr = scanner.nextLine().trim();
            if (portStr.isEmpty()) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: Port cannot be empty");
                return;
            }
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: Invalid port number");
                return;
            }
            
            System.out.print("Title: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String title = scanner.nextLine().trim();
            if (title.isEmpty()) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: Title cannot be empty");
                return;
            }
            
            File rfcDir = config.getRfcDirectory();
            File rfcFile = findRfcFileInDirectory(rfcDir, rfcNumber);
            
            if (rfcFile == null || !rfcFile.exists()) {
                System.out.println("P2P-CI/1.0 404 Not Found");
                System.err.println("Error: RFC " + rfcNumber + " not found in local directory: " + rfcDir.getAbsolutePath());
                return;
            }
            
            System.out.println("Found local file: " + rfcFile.getAbsolutePath());
            
            if (p2sClient.addRfc(rfcNumber, title, version)) {
                System.out.println("Successfully registered RFC " + rfcNumber + " with server");
            } else {
                System.err.println("Failed to register RFC " + rfcNumber + " with server");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid RFC number: " + parts[2]);
        }
    }
    private static void handleLookup(String[] parts, Scanner scanner) {
        if (parts.length < 4 || !parts[1].equalsIgnoreCase("RFC")) {
            System.out.println("Usage: LOOKUP RFC <number> P2P-CI/1.0");
            System.out.println("You will be prompted for Host, Port, and Title");
            return;
        }
        try {
            int rfcNumber = Integer.parseInt(parts[2]);
            String version = parts[3];
            
            System.out.print("Host: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String host = scanner.nextLine().trim();
            if (host.isEmpty()) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: Host cannot be empty");
                return;
            }
            
            System.out.print("Port: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String portStr = scanner.nextLine().trim();
            if (portStr.isEmpty()) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: Port cannot be empty");
                return;
            }
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: Invalid port number");
                return;
            }
            
            System.out.print("Title: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String title = scanner.nextLine().trim();
            if (title.isEmpty()) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: Title cannot be empty");
                return;
            }
            
            List<RfcRecord> records = p2sClient.lookupRfc(rfcNumber, version);
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
    private static void handleList(String[] parts, Scanner scanner) {
        if (parts.length < 3 || !parts[1].equalsIgnoreCase("ALL")) {
            System.out.println("Usage: LIST ALL P2P-CI/1.0");
            System.out.println("You will be prompted for Host and Port");
            return;
        }
        
        String version = parts[2];
        
        System.out.print("Host: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            System.out.println("P2P-CI/1.0 400 Bad Request");
            System.err.println("Error: Host cannot be empty");
            return;
        }
        
        System.out.print("Port: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String portStr = scanner.nextLine().trim();
        if (portStr.isEmpty()) {
            System.out.println("P2P-CI/1.0 400 Bad Request");
            System.err.println("Error: Port cannot be empty");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            System.out.println("P2P-CI/1.0 400 Bad Request");
            System.err.println("Error: Invalid port number");
            return;
        }
        
        List<RfcRecord> records = p2sClient.listAll(version);
        if (records.isEmpty()) {
            System.out.println("No RFCs found in the network");
        } else {
            System.out.println("All RFCs in network (" + records.size() + " total):");
            for (RfcRecord record : records) {
                System.out.println("  RFC " + record.getRfcNumber() + " " + record.getTitle() + 
                        " " + record.getHost() + " " + record.getUploadPort());
            }
        }
    }
    private static void handleGet(String[] parts, Scanner scanner) {
        if (parts.length < 4 || !parts[1].equalsIgnoreCase("RFC")) {
            System.out.println("Usage: GET RFC <number> P2P-CI/1.0");
            System.out.println("You will be prompted for Host and OS");
            return;
        }
        try {
            int rfcNumber = Integer.parseInt(parts[2]);
            String version = parts[3];
            
            System.out.print("Host: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String host = scanner.nextLine().trim();
            if (host.isEmpty()) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: Host cannot be empty");
                return;
            }
            
            System.out.print("OS: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String os = scanner.nextLine().trim();
            if (os.isEmpty()) {
                System.out.println("P2P-CI/1.0 400 Bad Request");
                System.err.println("Error: OS cannot be empty");
                return;
            }
            
            System.out.println("Looking up RFC " + rfcNumber + "...");
            List<RfcRecord> records = p2sClient.lookupRfc(rfcNumber, version);
            if (records.isEmpty()) {
                System.out.println("No peers found with RFC " + rfcNumber);
                return;
            }
            RfcRecord record = records.get(0);
            System.out.println("Downloading RFC " + rfcNumber + " from " +
                    record.getHost() + ":" + record.getUploadPort() + "...");
            PeerInfo peer = new PeerInfo(record.getHost(), record.getUploadPort());
            String title = record.getTitle();
            boolean success = p2pClient.downloadRfc(peer, rfcNumber, config.getRfcDirectory(), os, title, host);
            if (success) {
                System.out.println("Successfully downloaded RFC " + rfcNumber);
                if (p2sClient.addRfc(rfcNumber, title, "P2P-CI/1.0")) {
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
    private static void handleExit() {
        boolean success = p2sClient.exit();
        if (success) {
            System.out.println("Successfully exited peer");
            running = false;
        } else {
            System.err.println("Failed to exit peer");
            running = true;
        }
    }
}
