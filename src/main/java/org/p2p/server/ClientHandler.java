package org.p2p.server;
import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.List;
import org.p2p.common.RfcRecord;
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final PeerRegistry peerRegistry;
    private final RfcIndex rfcIndex;
    private String registeredHostname = null;
    private int registeredPort = -1;
    private boolean cleanupDone = false;
    public ClientHandler(Socket socket, PeerRegistry peerRegistry, RfcIndex rfcIndex) {
        this.socket = socket;
        this.peerRegistry = peerRegistry;
        this.rfcIndex = rfcIndex;
    }
    @Override
    public void run() {
        String peerHost = socket.getInetAddress().getHostAddress();
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            while(true) {
                String firstline = in.readLine();
                if (firstline == null) {
                    break;
                }
                System.out.println("Received from " + peerHost + ": " + firstline);
                StringTokenizer first_tokens = new StringTokenizer(firstline, " ");
                if (!first_tokens.hasMoreTokens()) {
                    sendBadRequest(out);
                    continue;
                }
                String method = first_tokens.nextToken();
                switch (method) {
                    case "ADD": {
                        if(!first_tokens.hasMoreTokens()) {
                            sendBadRequest(out);
                            break;
                        }
                        String literal = first_tokens.nextToken();
                        if (!literal.equals("RFC")) {
                            sendBadRequest(out);
                            break;
                        }
                        if(!first_tokens.hasMoreTokens()) {
                            sendBadRequest(out);
                            break;
                        }
                        String rfcNumber = first_tokens.nextToken();
                        if(!first_tokens.hasMoreTokens()) {
                            sendBadRequest(out);
                            break;
                        }
                        String version = first_tokens.nextToken();
                        if (!version.equals("P2P-CI/1.0")) {
                            sendVersionNotSupported(out, version);
                            break;
                        }
                        handleAdd(in, out, literal, rfcNumber, version, peerHost);
                        break;
                    }
                    case "LOOKUP": {
                        if(!first_tokens.hasMoreTokens()) {
                            sendBadRequest(out);
                            break;
                        }
                        String literal = first_tokens.nextToken();
                        if (!literal.equals("RFC")) {
                            sendBadRequest(out);
                            break;
                        }
                        if(!first_tokens.hasMoreTokens()) {
                            sendBadRequest(out);
                            break;
                        }
                        String rfcNumber = first_tokens.nextToken();
                        if(!first_tokens.hasMoreTokens()) {
                            sendBadRequest(out);
                            break;
                        }
                        String version = first_tokens.nextToken();
                        if (!version.equals("P2P-CI/1.0")) {
                            sendVersionNotSupported(out, version);
                            break;
                        }
                        handleLookUp(in, out, literal, rfcNumber, version);
                        break;
                    }
                    case "LIST": {
                        if(!first_tokens.hasMoreTokens()) {
                            sendBadRequest(out);
                            break;
                        }
                        String literal = first_tokens.nextToken();
                        if (!literal.equals("ALL")) {
                            sendBadRequest(out);
                            break;
                        }
                        if(!first_tokens.hasMoreTokens()) {
                            sendBadRequest(out);
                            break;
                        }
                        String version = first_tokens.nextToken();
                        if (!version.equals("P2P-CI/1.0")) {
                            sendVersionNotSupported(out, version);
                            break;
                        }
                        handleListAll(in, out, literal, version);
                        break;
                    }
                    case "EXIT": {
                        if(!first_tokens.hasMoreTokens()) {
                            sendBadRequest(out);
                            break;
                        }
                        String literal = first_tokens.nextToken();
                        if (!literal.equals("P2P-CI/1.0")) {
                            sendBadRequest(out);
                            break;
                        }
                        handleExit(in, out, literal);
                        break;
                    }
                    default: {
                        sendBadRequest(out);
                        break;
                    }
                }
            }
            
            if (!cleanupDone && registeredHostname != null && registeredPort != -1) {
                System.out.println("Peer " + registeredHostname + ":" + registeredPort + " (IP: " + peerHost + ") disconnected gracefully");
                peerRegistry.removePeer(registeredHostname);
                rfcIndex.removePeer(registeredHostname, registeredPort);
            }
        } catch (IOException e) {
            
            if (!cleanupDone && registeredHostname != null && registeredPort != -1) {
                System.out.println("Peer " + registeredHostname + ":" + registeredPort + " (IP: " + peerHost + ") disconnected with error: " + e.getMessage());
                peerRegistry.removePeer(registeredHostname);
                rfcIndex.removePeer(registeredHostname, registeredPort);
            }
        }
    }
    public void handleAdd(BufferedReader in, BufferedWriter out, String literal, String rfcNumber, String version, String peerHost) throws IOException {
        String secondline = in.readLine();
        if(secondline == null) {
            sendBadRequest(out);
            return;
        }
        StringTokenizer second_tokens = new StringTokenizer(secondline, " ");
        if(!second_tokens.hasMoreTokens() || !second_tokens.nextToken().equals("Host:")) {
            sendBadRequest(out);
            return;
        }
        if(!second_tokens.hasMoreTokens()) {
            sendBadRequest(out);
            return;
        }
        String host = second_tokens.nextToken();
        String thirdline = in.readLine();
        if(thirdline == null) {
            sendBadRequest(out);
            return;
        }
        StringTokenizer third_tokens = new StringTokenizer(thirdline, " ");
        if(!third_tokens.hasMoreTokens() || !third_tokens.nextToken().equals("Port:")) {
            sendBadRequest(out);
            return;
        }
        if(!third_tokens.hasMoreTokens()) {
            sendBadRequest(out);
            return;
        }
        String port = third_tokens.nextToken();
        String fourthLine = in.readLine();
        if(fourthLine == null) {
            sendBadRequest(out);
            return;
        }
        if (!fourthLine.startsWith("Title:")) {
            sendBadRequest(out);
            return;
        }
        String titleHeaderVal = null;
        int firstSpace = fourthLine.indexOf(' ');
        if (firstSpace == -1 || firstSpace >= fourthLine.length() - 1) {
            sendBadRequest(out);
            return;
        } 
        else {
            titleHeaderVal = fourthLine.substring(firstSpace + 1);
        }
        String fifthLine = in.readLine();
        if(fifthLine == null || !fifthLine.equals("")){
            sendBadRequest(out);
            return;
        }
        int rfcNumInteger = -1;
        int portInteger = -1;
        try{
            rfcNumInteger = Integer.parseInt(rfcNumber);
            portInteger = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            sendBadRequest(out);
            return;
        }
        if (registeredHostname == null) {
            registeredHostname = host;
            registeredPort = portInteger;
            System.out.println("[Server] Peer registered: " + host + ":" + portInteger);
        }
        peerRegistry.addPeer(host, portInteger);
        rfcIndex.addRfc(rfcNumInteger, titleHeaderVal, host, portInteger);
        out.write("P2P-CI/1.0 200 OK\r\n");
        out.write("RFC " + rfcNumInteger + " " + titleHeaderVal + " " + host + " " + portInteger + "\r\n");
        out.write("\r\n");
        out.flush();   
        return;
    }
    public void handleLookUp(BufferedReader in, BufferedWriter out, String literal, String rfcNumber, String version) throws IOException {
        String secondline = in.readLine();
        if(secondline == null) {
            sendBadRequest(out);
            return;
        }
        StringTokenizer second_tokens = new StringTokenizer(secondline, " ");
        if(!second_tokens.hasMoreTokens() || !second_tokens.nextToken().equals("Host:")) {
            sendBadRequest(out);
            return;
        }
        if(!second_tokens.hasMoreTokens()) {
            sendBadRequest(out);
            return;
        }
        String host = second_tokens.nextToken();
        String thirdline = in.readLine();
        if(thirdline == null) {
            sendBadRequest(out);
            return;
        }
        StringTokenizer third_tokens = new StringTokenizer(thirdline, " ");
        if(!third_tokens.hasMoreTokens() || !third_tokens.nextToken().equals("Port:")) {
            sendBadRequest(out);
            return;
        }
        if(!third_tokens.hasMoreTokens()) {
            sendBadRequest(out);
            return;
        }
        String port = third_tokens.nextToken();
        String fourthLine = in.readLine();
        if(fourthLine == null) {
            sendBadRequest(out);
            return;
        }
        if (!fourthLine.startsWith("Title:")) {
            sendBadRequest(out);
            return;
        }
        String titleHeaderVal = null;
        int firstSpace = fourthLine.indexOf(' ');
        if (firstSpace == -1 || firstSpace >= fourthLine.length() - 1) {
            sendBadRequest(out);
            return;
        } 
        else {
            titleHeaderVal = fourthLine.substring(firstSpace + 1);
        }
        String fifthLine = in.readLine();
        if(fifthLine == null || !fifthLine.equals("")){
            sendBadRequest(out);
            return;
        }
        int rfcNumInteger = -1;
        int portInteger = -1;
        try{
            rfcNumInteger = Integer.parseInt(rfcNumber);
            portInteger = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            sendBadRequest(out);
            return;
        }
        List<RfcRecord> rfcRecords = rfcIndex.lookup(rfcNumInteger);
        if(rfcRecords == null || rfcRecords.isEmpty()) {
            sendNotFound(out);
            return;
        }
        out.write("P2P-CI/1.0 200 OK\r\n");
        out.write("\r\n");
        for (RfcRecord rec : rfcRecords) {
            out.write("RFC " + rec.getRfcNumber() + " " + rec.getTitle() + " " + rec.getHost() + " " + rec.getUploadPort() + "\r\n");
        }
        out.write("\r\n");
        out.flush();
    }
    public void handleListAll(BufferedReader in, BufferedWriter out, String literal, String version) throws IOException {
        String secondline = in.readLine();
        if(secondline == null) {
            sendBadRequest(out);
            return;
        }
        StringTokenizer second_tokens = new StringTokenizer(secondline, " ");
        if(!second_tokens.hasMoreTokens() || !second_tokens.nextToken().equals("Host:")) {
            sendBadRequest(out);
            return;
        }
        if(!second_tokens.hasMoreTokens()) {
            sendBadRequest(out);
            return;
        }
        String host = second_tokens.nextToken();
        String thirdline = in.readLine();
        if(thirdline == null) {
            sendBadRequest(out);
            return;
        }
        StringTokenizer third_tokens = new StringTokenizer(thirdline, " ");
        if(!third_tokens.hasMoreTokens() || !third_tokens.nextToken().equals("Port:")) {
            sendBadRequest(out);
            return;
        }
        if(!third_tokens.hasMoreTokens()) {
            sendBadRequest(out);
            return;
        }
        String port = third_tokens.nextToken();
        String fourthLine = in.readLine();
        if(fourthLine == null || !fourthLine.equals("")){
            sendBadRequest(out);
            return;
        }
        int portInteger = -1;
        try{
            portInteger = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            sendBadRequest(out);
            return;
        }
        List<RfcRecord> rfcRecords = rfcIndex.listAll(host, portInteger);
        out.write("P2P-CI/1.0 200 OK\r\n");
        out.write("\r\n");
        if (rfcRecords != null && !rfcRecords.isEmpty()) {
            for (RfcRecord rec : rfcRecords) {
                out.write("RFC " + rec.getRfcNumber() + " " + rec.getTitle() + " " + rec.getHost() + " " + rec.getUploadPort() + "\r\n");
            }
        }
        out.write("\r\n");
        out.flush();
    }
    public void handleExit(BufferedReader in, BufferedWriter out, String literal) throws IOException {
        String secondline = in.readLine();
        if(secondline == null) {
            sendBadRequest(out);
            return;
        }
        StringTokenizer second_tokens = new StringTokenizer(secondline, " ");
        if(!second_tokens.hasMoreTokens() || !second_tokens.nextToken().equals("Host:")) {
            sendBadRequest(out);
            return;
        }
        if(!second_tokens.hasMoreTokens()) {
            sendBadRequest(out);
            return;
        }
        String host = second_tokens.nextToken();
        String thirdline = in.readLine();
        if(thirdline == null) {
            sendBadRequest(out);
            return;
        }
        StringTokenizer third_tokens = new StringTokenizer(thirdline, " ");
        if(!third_tokens.hasMoreTokens() || !third_tokens.nextToken().equals("Port:")) {
            sendBadRequest(out);
            return;
        }
        if(!third_tokens.hasMoreTokens()) {
            sendBadRequest(out);
            return;
        }
        String port = third_tokens.nextToken();
        String fourthLine = in.readLine();
        if(fourthLine == null || !fourthLine.equals("")){
            sendBadRequest(out);
            return;
        }
        int portInteger = -1;
        try{
            portInteger = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            sendBadRequest(out);
            return;
        }
        
        String hostToRemove = registeredHostname != null ? registeredHostname : host;
        int portToRemove = registeredPort != -1 ? registeredPort : portInteger;
        System.out.println("EXIT received from peer " + hostToRemove + ":" + portToRemove + " - performing cleanup...");
        
        rfcIndex.removePeer(hostToRemove, portToRemove);
        
        peerRegistry.removePeer(hostToRemove);
        
        cleanupDone = true;
        
        out.write("P2P-CI/1.0 200 OK\r\n");
        out.write("\r\n");
        out.flush();
        System.out.println("Peer " + hostToRemove + " cleanup complete. Connection will be closed.");
        
        try {
            socket.close();
        } catch (IOException e) {
            
        }
    }
    private void sendBadRequest(BufferedWriter out) throws IOException {
        out.write("P2P-CI/1.0 400 Bad Request\r\n");
        out.write("\r\n");
        out.flush();
    }
    private void sendVersionNotSupported(BufferedWriter out, String version) throws IOException {
        out.write("P2P-CI/1.0 505 P2P-CI Version Not Supported\r\n");
        out.write("\r\n");
        out.flush();
    }
    private void sendNotFound(BufferedWriter out) throws IOException {
        out.write("P2P-CI/1.0 404 Not Found\r\n");
        out.write("\r\n");
        out.flush();
    }
}
