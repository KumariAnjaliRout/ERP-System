package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long productId;
    private String productName;
    private String productImage;
    private String imageUrl;
    private Integer quantity;
    private Double unitPrice;

    // NEW FIELDS
    private Double discount;         // %
    private Double tax;              // %

    private Double discountAmount;
    private Double taxAmount;

    private Double totalPrice;
}