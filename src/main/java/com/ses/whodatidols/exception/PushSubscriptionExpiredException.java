package com.ses.whodatidols.exception;

public class PushSubscriptionExpiredException extends RuntimeException {
    public PushSubscriptionExpiredException(String message) {
        super(message);
    }
}
