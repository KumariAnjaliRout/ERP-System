package com.InventoryMgt.InventoryMgtProject.Expection;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(String message) {
        super(message);
    }
}