package org.p2p.server;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final PeerRegistry peerRegistry;
    private final RfcIndex rfcIndex;

    public ClientHandler(Socket socket, PeerRegistry peerRegistry, RfcIndex rfcIndex) {
        this.socket = socket;
        this.peerRegistry = peerRegistry;
        this.rfcIndex = rfcIndex;
    }

    @Override
    public void run() {
        String peerHost = socket.getInetAddress().getHostAddress();
        String method = null, literal = null, rfcNumber = null, version = null;

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            while(true) {
                String firstline = in.readLine();
                if (firstline == null) break;
                System.out.println("Received from " + peerHost + ": " + firstline);

                StringTokenizer first_tokens = new StringTokenizer(firstline, " ");

                if (!first_tokens.hasMoreTokens()) {
                    sendBadRequest(out);
                    continue;
                }

                if (first_tokens.hasMoreTokens()) {
                    method = first_tokens.nextToken();

                    switch (method) {
                        case "ADD":
                            if(!first_tokens.hasMoreTokens()) {
                                sendError(out, "400 Bad Request: Wrong Input");
                                break;
                            }
                            literal = first_tokens.nextToken();
                            ;
                            if (!literal.equals("RFC")) {
                                sendError(out, "400 Bad Request: missing RFC keyword");
                                break;
                            }

                            if(!first_tokens.hasMoreTokens()) {
                                sendError(out, "400 Bad Request: missing correct rfcNumber");
                                break;
                            }
                            rfcNumber = first_tokens.nextToken();

                            if(!first_tokens.hasMoreTokens()) {
                                sendError(out, "400 Bad Request: missing correct version");
                                break;
                            }
                            version = first_tokens.nextToken();

                            if (!version.equals("P2P-CI/1.0")) {
                                sendError(out, "P2P-CI/1.0 505 " + version + " Version Not Supported");
                                break;
                            }

                            addNew(in, out, literal, rfcNumber, version);
                            break;

                        case "LOOKUP":
                            if(!first_tokens.hasMoreTokens()) {
                                sendError(out, "400 Bad Request: Wrong Input");
                                break;
                            }
                            literal = first_tokens.nextToken();
                            ;
                            if (!literal.equals("RFC")) {
                                sendError(out, "400 Bad Request: missing RFC keyword");
                                break;
                            }

                            if(!first_tokens.hasMoreTokens()) {
                                sendError(out, "400 Bad Request: missing correct rfcNumber");
                                break;
                            }
                            rfcNumber = first_tokens.nextToken();

                            if(!first_tokens.hasMoreTokens()) {
                                sendError(out, "400 Bad Request: missing correct version");
                                break;
                            }
                            version = first_tokens.nextToken();

                            if (!version.equals("P2P-CI/1.0")) {
                                sendError(out, "P2P-CI/1.0 505 " + version + " Version Not Supported");
                                break;
                            }

                            lookUp(in, out, literal, rfcNumber, version);
                            break;

                        case "LIST":
                            if(!first_tokens.hasMoreTokens()) {
                                sendError(out, "400 Bad Request: Wrong Input");
                                break;
                            }
                            literal = first_tokens.nextToken();

                            if (!literal.equals("ALL")) {
                                sendError(out, "400 Bad Request: missing RFC keyword");
                                break;
                            }

                            if(!first_tokens.hasMoreTokens()) {
                                sendError(out, "400 Bad Request: missing correct version");
                                break;
                            }
                            version = first_tokens.nextToken();

                            if (!version.equals("P2P-CI/1.0")) {
                                sendError(out, "P2P-CI/1.0 505 " + version + " Version Not Supported");
                                break;
                            }

                            listAll(in, out, literal, version);

                            break;
                        default: {
                            sendError(out, "400 Bad Request: unknown method");
                            break;
                        }
                    }
                }
            }


            out.write("P2P-CI/1.0 200 OK\r\n");
            out.write("This is a placeholder response from server.\r\n");
            out.write("\r\n");
            out.flush();

            // Later:
            // 1. parse full request (method, headers)
            // 2. handle ADD / LOOKUP / LIST ALL
            // 3. update peerRegistry, rfcIndex
            // 4. send proper formatted response (including RFC list)

        } catch (IOException e) {
            System.out.println("Peer " + peerHost + " disconnected: " + e.getMessage());
        } finally {
            // cleanup this peer's RFCs and entry when it disconnects
            peerRegistry.removePeer(peerHost);
            rfcIndex.removeHost(peerHost);
        }
    }

    public void addNew(BufferedReader in, BufferedWriter out, String literal, String rfcNumber, String version) throws IOException {
        String secondline = in.readLine();
        StringTokenizer second_tokens = new StringTokenizer(secondline, " ");
        String host = null, port = null, title = null;

        int i = 2;
        while(i-- >= 1){
            if(!second_tokens.hasMoreTokens()) {
                sendError(out, "400 Bad Request: missing correct version");
                break;
            }

            second_tokens.nextToken();
        }
        host = second_tokens.nextToken();

        String thirdline = in.readLine();
        StringTokenizer third_tokens = new StringTokenizer(thirdline, " ");

        i = 2;
        while(i-- >= 1){
            if(!third_tokens.hasMoreTokens()) {
                sendError(out, "400 Bad Request: missing correct version");
                break;
            }

            third_tokens.nextToken();
        }
        port = third_tokens.nextToken();

        String fourthLine = in.readLine();
        StringTokenizer fourth_tokens = new StringTokenizer(fourthLine, " ");

        i = 2;
        while(i-- >= 1){
            if(!fourth_tokens.hasMoreTokens()) {
                sendError(out, "400 Bad Request: missing correct version");
                break;
            }

            fourth_tokens.nextToken();
        }
        title = fourth_tokens.nextToken();

        String fifthLine = in.readLine();
        if(!fifthLine.equals("")){
            sendError(out, literal + " 400 Bad Request");
        }
    }

    public void lookUp(BufferedReader in, BufferedWriter out, String literal, String rfcNumber, String version) throws IOException{
        String secondline = in.readLine();
        StringTokenizer second_tokens = new StringTokenizer(secondline, " ");
        String host = null, port = null, title = null;

        int i = 2;
        while(i-- >= 1){
            if(!second_tokens.hasMoreTokens()) {
                sendError(out, "400 Bad Request: missing correct version");
                break;
            }

            second_tokens.nextToken();
        }
        host = second_tokens.nextToken();

        String thirdline = in.readLine();
        StringTokenizer third_tokens = new StringTokenizer(thirdline, " ");

        i = 2;
        while(i-- >= 1){
            if(!third_tokens.hasMoreTokens()) {
                sendError(out, "400 Bad Request: missing correct version");
                break;
            }

            third_tokens.nextToken();
        }
        port = third_tokens.nextToken();

        String fourthLine = in.readLine();
        if(!fourthLine.equals("")){
            sendError(out, literal + " 400 Bad Request");
        }
    }

    public void listAll(BufferedReader in, BufferedWriter out, String literal, String version) throws IOException{
        String secondline = in.readLine();
        StringTokenizer second_tokens = new StringTokenizer(secondline, " ");
        String host = null, port = null, title = null;

        int i = 2;
        while(i-- >= 1){
            if(!second_tokens.hasMoreTokens()) {
                sendError(out, "400 Bad Request: missing correct version");
                break;
            }

            second_tokens.nextToken();
        }
        host = second_tokens.nextToken();

        String thirdline = in.readLine();
        StringTokenizer third_tokens = new StringTokenizer(thirdline, " ");

        i = 2;
        while(i-- >= 1){
            if(!third_tokens.hasMoreTokens()) {
                sendError(out, "400 Bad Request: missing correct version");
                break;
            }

            third_tokens.nextToken();
        }
        port = third_tokens.nextToken();

        String fourthLine = in.readLine();
        if(!fourthLine.equals("")){
            sendError(out, literal + " 400 Bad Request");
        }
    }
}