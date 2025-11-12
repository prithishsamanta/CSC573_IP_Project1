package org.p2p.peer;

import java.io.File;

/**
 * Simple configuration holder for a peer instance.
 */
public class PeerConfig {

    private final String serverHost;
    private final int serverPort;
    private final int uploadPort;
    private final File rfcDirectory;
    private final String osName;

    public PeerConfig(String serverHost, int serverPort, int uploadPort, File rfcDirectory, String osName) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.uploadPort = uploadPort;
        this.rfcDirectory = rfcDirectory;
        this.osName = osName;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getUploadPort() {
        return uploadPort;
    }

    public File getRfcDirectory() {
        return rfcDirectory;
    }

    public String getOsName() {
        return osName;
    }

    /**
     * Very simple CLI parser:
     *   --serverHost <host>   (default: localhost)
     *   --serverPort <port>   (default: 7734)
     *   --uploadPort <port>   (default: 0 -> choose random free port)
     *   --rfcDir <path>       (default: ./rfc)
     *   --os "<os string>"    (default: System.getProperty("os.name"))
     */
    public static PeerConfig fromArgs(String[] args) {
        String serverHost = "localhost";
        int serverPort = 7734;
        int uploadPort = 0;
        File rfcDir = new File("rfc");
        String osName = System.getProperty("os.name");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--serverHost":
                    if (i + 1 < args.length) serverHost = args[++i];
                    break;
                case "--serverPort":
                    if (i + 1 < args.length) serverPort = Integer.parseInt(args[++i]);
                    break;
                case "--uploadPort":
                    if (i + 1 < args.length) uploadPort = Integer.parseInt(args[++i]);
                    break;
                case "--rfcDir":
                    if (i + 1 < args.length) rfcDir = new File(args[++i]);
                    break;
                case "--os":
                    if (i + 1 < args.length) osName = args[++i];
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
            }
        }

        if (!rfcDir.exists()) {
            // You can also choose to create the directory automatically
            rfcDir.mkdirs();
        }

        return new PeerConfig(serverHost, serverPort, uploadPort, rfcDir, osName);
    }
}
