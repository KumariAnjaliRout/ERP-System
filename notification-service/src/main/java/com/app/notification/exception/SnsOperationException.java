package com.app.notification.exception;

public class SnsOperationException extends RuntimeException {

    public SnsOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}