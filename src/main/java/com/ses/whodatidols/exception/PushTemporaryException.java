package com.ses.whodatidols.exception;

public class PushTemporaryException extends RuntimeException {
    private final int statusCode;

    public PushTemporaryException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
