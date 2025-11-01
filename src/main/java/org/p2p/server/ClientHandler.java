package org.p2p.server;

import java.io.*;
import java.net.Socket;

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

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())
                )
        ) {
            String line = in.readLine();
            System.out.println("Received from " + peerHost + ": " + line);

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
}