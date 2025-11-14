package org.p2p.peer;

import org.p2p.common.PeerInfo;
import org.p2p.common.RfcRecord;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class P2SClient {
    private final String serverHost;
    private final int serverPort;
    private final String peerHost;
    private final int uploadPort;
    private final String osName;
    
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private volatile boolean connected = false;

    public P2SClient(String serverHost, int serverPort, String peerHost, int uploadPort, String osName) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.peerHost = peerHost;
        this.uploadPort = uploadPort;
        this.osName = osName;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            connected = true;
            System.out.println("[P2SClient] Connected to server at " + serverHost + ":" + serverPort);
            return true;
        } catch (IOException e) {
            System.err.println("[P2SClient] Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    public boolean addRfc(int rfcNumber, String title) {
        if (!connected || socket == null || socket.isClosed()) {
            System.err.println("[P2SClient] Not connected to server");
            return false;
        }

        try {
            out.write("ADD RFC " + rfcNumber + " P2P-CI/1.0\r\n");
            out.write("Host: " + peerHost + "\r\n");
            out.write("Port: " + uploadPort + "\r\n");
            out.write("Title: " + title + "\r\n");
            out.write("\r\n");
            out.flush();

            String statusLine = in.readLine();
            if (statusLine == null) {
                System.err.println("[P2SClient] No response from server for ADD");
                return false;
            }

            System.out.println("[P2SClient] ADD response: " + statusLine);

            String echoLine = in.readLine();
            if (echoLine != null && !echoLine.isEmpty()) {
                System.out.println("[P2SClient] " + echoLine);
            }

            String blankLine = in.readLine();
            
            if (statusLine.startsWith("P2P-CI/1.0 200")) {
                return true;
            } else {
                System.err.println("[P2SClient] ADD failed: " + statusLine);
                return false;
            }
        } catch (IOException e) {
            System.err.println("[P2SClient] Error sending ADD: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    public List<RfcRecord> lookupRfc(int rfcNumber) {
        if (!connected || socket == null || socket.isClosed()) {
            System.err.println("[P2SClient] Not connected to server");
            return new ArrayList<>();
        }

        try {
            out.write("LOOKUP RFC " + rfcNumber + " P2P-CI/1.0\r\n");
            out.write("Host: " + peerHost + "\r\n");
            out.write("Port: " + uploadPort + "\r\n");
            out.write("Title: RFC " + rfcNumber + "\r\n");
            out.write("\r\n");
            out.flush();

            String statusLine = in.readLine();
            if (statusLine == null) {
                System.err.println("[P2SClient] No response from server for LOOKUP");
                return new ArrayList<>();
            }

            System.out.println("[P2SClient] LOOKUP response: " + statusLine);

            List<RfcRecord> records = new ArrayList<>();

            if (statusLine.startsWith("P2P-CI/1.0 200")) {
                // Read blank line after status
                in.readLine();
                
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 5 && parts[0].equals("RFC")) {
                        try {
                            int rfcNum = Integer.parseInt(parts[1]);
                            int port = Integer.parseInt(parts[parts.length - 1]);
                            String host = parts[parts.length - 2];
                            StringBuilder titleBuilder = new StringBuilder();
                            for (int i = 2; i < parts.length - 2; i++) {
                                if (i > 2) titleBuilder.append(" ");
                                titleBuilder.append(parts[i]);
                            }
                            String title = titleBuilder.toString();
                            records.add(new RfcRecord(rfcNum, title, host, port));
                        } catch (NumberFormatException e) {
                            System.err.println("[P2SClient] Error parsing LOOKUP response line: " + line);
                        }
                    }
                }
            } else if (statusLine.startsWith("P2P-CI/1.0 404")) {
                in.readLine();
                System.out.println("[P2SClient] RFC " + rfcNumber + " not found");
            } else {
                in.readLine();
                System.err.println("[P2SClient] LOOKUP failed: " + statusLine);
            }

            return records;
        } catch (IOException e) {
            System.err.println("[P2SClient] Error sending LOOKUP: " + e.getMessage());
            connected = false;
            return new ArrayList<>();
        }
    }

    public List<RfcRecord> listAll() {
        if (!connected || socket == null || socket.isClosed()) {
            System.err.println("[P2SClient] Not connected to server");
            return new ArrayList<>();
        }

        try {
            out.write("LIST ALL P2P-CI/1.0\r\n");
            out.write("Host: " + peerHost + "\r\n");
            out.write("Port: " + uploadPort + "\r\n");
            out.write("\r\n");
            out.flush();

            String statusLine = in.readLine();
            if (statusLine == null) {
                System.err.println("[P2SClient] No response from server for LIST");
                return new ArrayList<>();
            }

            System.out.println("[P2SClient] LIST response: " + statusLine);

            List<RfcRecord> records = new ArrayList<>();

            if (statusLine.startsWith("P2P-CI/1.0 200")) {
                // Read blank line after status
                in.readLine();
                
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 5 && parts[0].equals("RFC")) {
                        try {
                            int rfcNum = Integer.parseInt(parts[1]);
                            int port = Integer.parseInt(parts[parts.length - 1]);
                            String host = parts[parts.length - 2];
                            StringBuilder titleBuilder = new StringBuilder();
                            for (int i = 2; i < parts.length - 2; i++) {
                                if (i > 2) titleBuilder.append(" ");
                                titleBuilder.append(parts[i]);
                            }
                            String title = titleBuilder.toString();
                            records.add(new RfcRecord(rfcNum, title, host, port));
                        } catch (NumberFormatException e) {
                            System.err.println("[P2SClient] Error parsing LIST response line: " + line);
                        }
                    }
                }
            } else {
                in.readLine();
                System.err.println("[P2SClient] LIST failed: " + statusLine);
            }

            return records;
        } catch (IOException e) {
            System.err.println("[P2SClient] Error sending LIST: " + e.getMessage());
            connected = false;
            return new ArrayList<>();
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
        }
        System.out.println("[P2SClient] Disconnected from server");
    }
}
