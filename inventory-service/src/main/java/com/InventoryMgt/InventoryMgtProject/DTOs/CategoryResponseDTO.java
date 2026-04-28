package com.InventoryMgt.InventoryMgtProject.DTOs;

import com.InventoryMgt.InventoryMgtProject.Entities.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private Status status;
};
