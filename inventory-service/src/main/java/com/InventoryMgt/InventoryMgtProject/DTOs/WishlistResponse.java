package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {
    private Long wishlistId;
    //private String outletId;
    private Integer itemCount;
    private List<WishlistItemResponse> items;
}