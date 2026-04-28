package com.app.notification.exception;

public class NotificationQueryException extends RuntimeException {

    public NotificationQueryException(String message) {
        super(message);
    }

    public NotificationQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}