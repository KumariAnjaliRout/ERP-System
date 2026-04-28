package com.InventoryMgt.InventoryMgtProject.Expection;

public class InvoiceGenerationException extends RuntimeException {

    public InvoiceGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}