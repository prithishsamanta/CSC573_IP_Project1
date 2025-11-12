package org.p2p.peer;

import java.io.File;
import java.io.IOException;

public class PeerMain {

    public static void main(String[] args) {
        PeerConfig config = PeerConfig.fromArgs(args);

        System.out.println("Starting peer with config:");
        System.out.println("  RFC directory : " + config.getRfcDirectory().getAbsolutePath());
        System.out.println("  Upload port   : " + (config.getUploadPort() == 0 ? "auto (ephemeral)" : config.getUploadPort()));
        System.out.println("  OS            : " + config.getOsName());

        UploadServer uploadServer = new UploadServer(config.getUploadPort(), config.getRfcDirectory(), config.getOsName());
        Thread uploadThread = new Thread(uploadServer, "UploadServer");
        uploadThread.start();

        int boundPort = uploadServer.waitForBoundPort();
        System.out.println("Upload server listening on port " + boundPort);
        try {
            uploadThread.join();
        } catch (InterruptedException e) {
            System.err.println("PeerMain interrupted, shutting down...");
            Thread.currentThread().interrupt();
        }
    }
}
