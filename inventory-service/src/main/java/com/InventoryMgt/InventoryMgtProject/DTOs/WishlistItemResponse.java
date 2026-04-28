package com.InventoryMgt.InventoryMgtProject.DTOs;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private String imageUrl;
    private Double price;
    private Instant addedAt;
}
