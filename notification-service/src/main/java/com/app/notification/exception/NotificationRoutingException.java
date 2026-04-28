package com.app.notification.exception;

public class NotificationRoutingException extends RuntimeException {

    public NotificationRoutingException(String message) {
        super(message);
    }

    public NotificationRoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}