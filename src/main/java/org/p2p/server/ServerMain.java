package org.p2p.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    public static final int SERVER_PORT = 7734;

    public static void main(String[] args) {
        PeerRegistry peerRegistry = new PeerRegistry();
        RfcIndex rfcIndex = new RfcIndex();

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("P2P-CI Server listening on port " + SERVER_PORT);

            ExecutorService pool = Executors.newCachedThreadPool();

            while (true) {
                Socket peerSocket = serverSocket.accept();
                System.out.println("New peer connected: " + peerSocket.getRemoteSocketAddress());

                // hand off to worker
                ClientHandler handler = new ClientHandler(peerSocket, peerRegistry, rfcIndex);
                pool.submit(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}