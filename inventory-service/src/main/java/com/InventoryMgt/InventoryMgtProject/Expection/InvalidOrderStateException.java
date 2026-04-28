package com.InventoryMgt.InventoryMgtProject.Expection;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}