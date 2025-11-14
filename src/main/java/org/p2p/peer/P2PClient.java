package org.p2p.peer;

import org.p2p.common.PeerInfo;
import org.p2p.common.StatusCode;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class P2PClient {
    public boolean downloadRfc(PeerInfo peer, int rfcNumber, File targetDir, String osName) {
        try (Socket socket = new Socket(peer.getHost(), peer.getUploadPort())) {

            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            out.write("GET RFC " + rfcNumber + " P2P-CI/1.0\r\n");
            out.write("Host: " + peer.getHost() + "\r\n");
            out.write("OS: " + osName + "\r\n");
            out.write("\r\n");
            out.flush();

            InputStream rawIn = socket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(rawIn);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(bis, StandardCharsets.UTF_8));

            String statusLine = in.readLine();
            if (statusLine == null) {
                System.err.println("[P2PClient] No response from peer");
                return false;
            }

            System.out.println("[P2PClient] Status: " + statusLine);

            int contentLength = -1;

            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                System.out.println("[P2PClient] Header: " + headerLine);
                if (headerLine.toLowerCase().startsWith("content-length:")) {
                    String value = headerLine.substring("content-length:".length()).trim();
                    contentLength = Integer.parseInt(value);
                }
            }

            if (!statusLine.startsWith(StatusCode.OK_200)) {
                if (statusLine.startsWith(StatusCode.NOT_FOUND_404)) {
                    System.err.println("[P2PClient] RFC " + rfcNumber + " not found on peer");
                } else if (statusLine.startsWith(StatusCode.BAD_REQUEST_400)) {
                    System.err.println("[P2PClient] Bad request");
                } else if (statusLine.startsWith(StatusCode.VERSION_NOT_SUPPORTED_505)) {
                    System.err.println("[P2PClient] Version not supported");
                } else {
                    System.err.println("[P2PClient] Unexpected status: " + statusLine);
                }
                return false;
            }

            if (contentLength < 0) {
                System.err.println("[P2PClient] Missing Content-Length header");
                return false;
            }

            byte[] body = readBytes(bis, contentLength);

            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            File outFile = new File(targetDir, "rfc" + rfcNumber + ".txt");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(body);
            }

            System.out.println("[P2PClient] Saved RFC " + rfcNumber +
                    " to " + outFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            System.err.println("[P2PClient] I/O error: " + e.getMessage());
            return false;
        }
    }

    private static byte[] readBytes(InputStream in, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(data, offset, length - offset);
            if (read == -1) {
                throw new EOFException("Unexpected end of stream");
            }
            offset += read;
        }
        return data;
    }
}
