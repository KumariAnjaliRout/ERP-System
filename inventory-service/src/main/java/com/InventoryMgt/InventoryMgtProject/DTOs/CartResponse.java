package com.InventoryMgt.InventoryMgtProject.DTOs;


import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long cartId;

    private String outletId;

    private Double totalAmount;

    private Integer itemCount;

    private List<CartItemResponse> items;
}