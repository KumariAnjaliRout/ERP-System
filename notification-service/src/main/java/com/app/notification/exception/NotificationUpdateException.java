package com.app.notification.exception;

public class NotificationUpdateException extends RuntimeException {

    public NotificationUpdateException(String message) {
        super(message);
    }

    public NotificationUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}