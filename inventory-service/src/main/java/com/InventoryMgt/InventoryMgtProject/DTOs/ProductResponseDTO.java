package com.InventoryMgt.InventoryMgtProject.DTOs;

import com.InventoryMgt.InventoryMgtProject.Entities.ProductStatus;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponseDTO {

    private Long id;
    private String name;
    private String productImage;
    private Long categoryId;
    private Double price;
    private Double discount;
    private Double tax;
    private Integer quantity;
    private String imageUrl;
    private Double totalPrice;
    private ProductStatus productStatus;

}
