


    package com.InventoryMgt.InventoryMgtProject.DTOs;

import com.InventoryMgt.InventoryMgtProject.Entities.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class CategoryWithFullProductsDTO {

        private Long categoryId;
        private String categoryName;
        private List<ProductDTO> products;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class ProductDTO {
            private Long id;
            private String name;
            private String productImage;
            private Double price;
            private Double discount;
            private Double tax;
            private Integer quantity;
            private Double totalPrice;
            private String imageUrl;
            private ProductStatus productStatus;
        }
    }
