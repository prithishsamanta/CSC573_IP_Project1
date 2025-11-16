package org.p2p.common;
public class RfcRecord {
    private final int rfcNumber;
    private final String title;
    private final String host;
    private final int uploadPort;
    public RfcRecord(int rfcNumber, String title, String host, int uploadPort) {
        this.rfcNumber = rfcNumber;
        this.title = title;
        this.host = host;
        this.uploadPort = uploadPort;
    }
    public int getRfcNumber() {
        return rfcNumber;
    }
    public String getTitle() {
        return title;
    }
    public String getHost() {
        return host;
    }
    public int getUploadPort() {
        return uploadPort;
    }
    @Override
    public String toString() {
        return "RfcRecord{" +
                "rfcNumber=" + rfcNumber +
                ", title='" + title + '\'' +
                ", host='" + host + '\'' +
                ", uploadPort=" + uploadPort +
                '}';
    }
}
