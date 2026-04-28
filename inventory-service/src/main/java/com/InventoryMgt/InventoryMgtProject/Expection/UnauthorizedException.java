package com.InventoryMgt.InventoryMgtProject.Expection;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}