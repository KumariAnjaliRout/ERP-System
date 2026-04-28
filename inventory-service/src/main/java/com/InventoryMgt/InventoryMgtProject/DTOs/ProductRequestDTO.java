package com.InventoryMgt.InventoryMgtProject.DTOs;

import com.InventoryMgt.InventoryMgtProject.Entities.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequestDTO {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 50, message = "Product name must be 2-50 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9 ]+$",
            message = "Product name can only contain letters, numbers, and spaces"
    )
    private String name;

    private String productImage;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private Double price;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @PositiveOrZero(message = "Discount cannot be negative")
    private Double discount;

    @PositiveOrZero(message = "Tax cannot be negative")
    private Double tax;

    @PositiveOrZero(message = "Quantity cannot be negative")
    private Integer quantity;

    private ProductStatus productStatus;
}
