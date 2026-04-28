package com.InventoryMgt.InventoryMgtProject.Expection;

public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}