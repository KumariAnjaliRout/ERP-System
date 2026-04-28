package com.InventoryMgt.InventoryMgtProject.DTOs;

import com.InventoryMgt.InventoryMgtProject.Entities.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDTO {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Category name must be 2-50 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9 ]+$",
            message = "Category name can only contain letters, numbers, and spaces"
    )
    private String name;

    private Status status;

    private String outletId;
}