package com.InventoryMgt.InventoryMgtProject.Expection;


public class CartInactiveException extends RuntimeException {

    public CartInactiveException(String message) {
        super(message);
    }
}