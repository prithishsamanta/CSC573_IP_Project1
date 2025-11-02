package org.p2p.common;

public final class StatusCode {
    public static final String OK_200 = "P2P-CI/1.0 200 OK";
    public static final String BAD_REQUEST_400 = "P2P-CI/1.0 400 Bad Request";
    public static final String NOT_FOUND_404 = "P2P-CI/1.0 404 Not Found";
    public static final String VERSION_NOT_SUPPORTED_505 = "P2P-CI/1.0 505 P2P-CI Version Not Supported";

    private StatusCode() {} // prevent instantiation
}

