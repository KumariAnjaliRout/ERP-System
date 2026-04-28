package com.InventoryMgt.InventoryMgtProject.Expection;

public class WishlistItemNotFoundException extends RuntimeException {

    public WishlistItemNotFoundException(String message) {
        super(message);
    }
}