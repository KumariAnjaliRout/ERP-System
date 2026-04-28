package com.InventoryMgt.InventoryMgtProject.Expection;

public class InvoiceUploadException extends RuntimeException {

    public InvoiceUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}