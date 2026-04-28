package com.app.notification.exception;

public class NotificationCreationException extends RuntimeException {

    public NotificationCreationException(String message) {
        super(message);
    }

    public NotificationCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}