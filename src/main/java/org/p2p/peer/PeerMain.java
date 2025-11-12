package org.p2p.peer;

import java.io.File;
import java.io.IOException;

/**
 * Entry point for the peer process.
 *
 * Responsibilities (for now):
 *  - Parse basic CLI args
 *  - Start the UploadServer on a given or random port
 *  - Print the chosen upload port so you can register it with the central server later
 *
 * Later you will add:
 *  - A control connection to the central server (P2S client)
 *  - A simple menu / commands for ADD, LOOKUP, LIST, GET, etc.
 */
public class PeerMain {

    public static void main(String[] args) {
        PeerConfig config = PeerConfig.fromArgs(args);

        System.out.println("Starting peer with config:");
        System.out.println("  RFC directory : " + config.getRfcDirectory().getAbsolutePath());
        System.out.println("  Upload port   : " + (config.getUploadPort() == 0 ? "auto (ephemeral)" : config.getUploadPort()));
        System.out.println("  OS            : " + config.getOsName());

        // Start the upload server in its own thread
        UploadServer uploadServer = new UploadServer(config.getUploadPort(), config.getRfcDirectory(), config.getOsName());
        Thread uploadThread = new Thread(uploadServer, "UploadServer");
        uploadThread.start();

        // Wait until the server has actually bound to a port
        int boundPort = uploadServer.waitForBoundPort();
        System.out.println("Upload server listening on port " + boundPort);

        // 🔜 Later:
        //  - Open TCP connection to central server (serverHost, serverPort)
        //  - Send ADD for each RFC in rfcDirectory using that boundPort
        //  - Provide CLI menu for LOOKUP / LIST / GET

        // For now, just keep the main thread alive
        try {
            uploadThread.join();
        } catch (InterruptedException e) {
            System.err.println("PeerMain interrupted, shutting down...");
            Thread.currentThread().interrupt();
        }
    }
}
