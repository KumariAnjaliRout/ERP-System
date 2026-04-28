package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.*;



// Response DTOs
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private String imageUrl;
    private Double unitPrice;
    private Integer quantity;
    private Double totalPrice;
}


