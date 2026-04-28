package com.InventoryMgt.InventoryMgtProject.Expection;

public class InvoiceNotGeneratedException extends RuntimeException {

    public InvoiceNotGeneratedException(String message) {
        super(message);
    }
}