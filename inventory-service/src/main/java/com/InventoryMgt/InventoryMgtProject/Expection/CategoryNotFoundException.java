package com.InventoryMgt.InventoryMgtProject.Expection;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String message) {
        super(message);
    }
}