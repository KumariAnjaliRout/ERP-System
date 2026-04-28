package com.InventoryMgt.InventoryMgtProject.Expection;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}