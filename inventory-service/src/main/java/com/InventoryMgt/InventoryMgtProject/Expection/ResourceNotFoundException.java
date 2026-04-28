package com.InventoryMgt.InventoryMgtProject.Expection;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}