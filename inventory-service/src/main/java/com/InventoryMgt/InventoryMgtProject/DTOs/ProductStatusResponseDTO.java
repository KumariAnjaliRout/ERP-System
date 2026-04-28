package com.InventoryMgt.InventoryMgtProject.DTOs;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductStatusResponseDTO {
    private Integer totalProducts;
    private Integer inStock;
    private Integer lowStock;
    private Integer outOfStock;
}
