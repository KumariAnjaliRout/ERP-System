package com.erp.accountantservice.dto;

import lombok.Data;

@Data
public class ProductItem {
    private int productId;
    private String productName;
    private int quantity;
    private double unitPrice;
    private double totalPrice;
}
