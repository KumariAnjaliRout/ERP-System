package com.InventoryMgt.InventoryMgtProject.Expection;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String message) {
        super(message);
    }
}