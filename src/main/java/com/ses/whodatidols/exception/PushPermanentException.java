package com.ses.whodatidols.exception;

public class PushPermanentException extends RuntimeException {
    private final int statusCode;

    public PushPermanentException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
