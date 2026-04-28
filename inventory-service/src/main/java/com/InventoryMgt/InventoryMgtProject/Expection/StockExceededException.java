package com.InventoryMgt.InventoryMgtProject.Expection;

public class StockExceededException extends RuntimeException {

    public StockExceededException(String message) {
        super(message);
    }
}