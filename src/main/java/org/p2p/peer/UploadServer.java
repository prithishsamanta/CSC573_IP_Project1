package org.p2p.peer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class UploadServer implements Runnable {

    private final int requestedPort;
    private final File rfcDirectory;
    private final String osName;

    private volatile int boundPort = -1;
    private volatile boolean running = true;

    public UploadServer(int requestedPort, File rfcDirectory, String osName) {
        this.requestedPort = requestedPort;
        this.rfcDirectory = rfcDirectory;
        this.osName = osName;
    }

    public int getBoundPort() {
        return boundPort;
    }

    public int waitForBoundPort() {
        while (boundPort == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return boundPort;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(requestedPort)) {
            this.boundPort = serverSocket.getLocalPort();
            System.out.println("[UploadServer] Bound to port " + boundPort +
                               ", serving RFCs from: " + rfcDirectory.getAbsolutePath());

            while (running) {
                Socket clientSocket = serverSocket.accept();
                Thread worker = new Thread(new UploadWorker(clientSocket, rfcDirectory, osName),
                                           "UploadWorker-" + clientSocket.getRemoteSocketAddress());
                worker.start();
            }
        } catch (IOException e) {
            System.err.println("[UploadServer] Error: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
    }
}

class UploadWorker implements Runnable {

    private final Socket socket;
    private final File rfcDirectory;
    private final String osName;

    public UploadWorker(Socket socket, File rfcDirectory, String osName) {
        this.socket = socket;
        this.rfcDirectory = rfcDirectory;
        this.osName = osName;
    }

    @Override
    public void run() {
        System.out.println("[UploadWorker] Connection from " + socket.getRemoteSocketAddress());
        try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendSimpleResponse(out, 400, "Bad Request");
                return;
            }

            String[] parts = requestLine.trim().split("\\s+");
            if (parts.length != 4 || !"GET".equals(parts[0]) || !"RFC".equals(parts[1])) {
                sendSimpleResponse(out, 400, "Bad Request");
                return;
            }

            String rfcNumber = parts[2];
            String version = parts[3];

            if (!"P2P-CI/1.0".equals(version)) {
                sendSimpleResponse(out, 505, "P2P-CI Version Not Supported");
                return;
            }

            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
            }

            File rfcFile = new File(rfcDirectory, "rfc" + rfcNumber + ".txt");
            if (!rfcFile.exists() || !rfcFile.isFile()) {
                sendSimpleResponse(out, 404, "Not Found");
                return;
            }

            byte[] fileBytes = readAllBytes(rfcFile);

            String now = httpDate(new Date());
            String lastModified = httpDate(new Date(rfcFile.lastModified()));

            out.write("P2P-CI/1.0 200 OK\r\n");
            out.write("Date: " + now + "\r\n");
            out.write("OS: " + osName + "\r\n");
            out.write("Last-Modified: " + lastModified + "\r\n");
            out.write("Content-Length: " + fileBytes.length + "\r\n");
            out.write("Content-Type: text/plain\r\n");
            out.write("\r\n"); 
            out.flush();

            OutputStream rawOut = socket.getOutputStream();
            rawOut.write(fileBytes);
            rawOut.flush();

            System.out.println("[UploadWorker] Successfully served RFC " + rfcNumber);

        } catch (IOException e) {
            System.err.println("[UploadWorker] I/O error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) {}
        }
    }

    private void sendSimpleResponse(BufferedWriter out, int code, String phrase) throws IOException {
        out.write("P2P-CI/1.0 " + code + " " + phrase + "\r\n");
        out.write("OS: " + osName + "\r\n");
        out.write("\r\n");
        out.flush();
        System.out.println("[UploadWorker] Sent error " + code + " " + phrase);
    }

    private static byte[] readAllBytes(File file) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        }
    }

    private static String httpDate(Date date) {
        SimpleDateFormat fmt =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        return fmt.format(date);
    }
}
