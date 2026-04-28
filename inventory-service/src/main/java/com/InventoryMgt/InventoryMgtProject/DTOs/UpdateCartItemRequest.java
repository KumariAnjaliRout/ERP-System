package com.InventoryMgt.InventoryMgtProject.DTOs;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class UpdateCartItemRequest {
    private Long cartItemId;
    private int quantity;
}
