package com.InventoryMgt.InventoryMgtProject.Expection;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(String message) {
        super(message);
    }
}