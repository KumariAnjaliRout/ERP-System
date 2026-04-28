package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//Used for analytics
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDemandStats {

    private Long productId;
    private String productName;
    private Long totalOrders;
    private Integer stockLeft;

}