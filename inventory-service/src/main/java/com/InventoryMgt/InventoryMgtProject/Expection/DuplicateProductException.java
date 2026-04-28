package com.InventoryMgt.InventoryMgtProject.Expection;

public class DuplicateProductException extends RuntimeException {
    public DuplicateProductException(String message) {
        super(message);
    }
}