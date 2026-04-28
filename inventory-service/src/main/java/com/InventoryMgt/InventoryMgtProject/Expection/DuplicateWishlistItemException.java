package com.InventoryMgt.InventoryMgtProject.Expection;

public class DuplicateWishlistItemException extends RuntimeException {

    public DuplicateWishlistItemException(String message) {
        super(message);
    }
}