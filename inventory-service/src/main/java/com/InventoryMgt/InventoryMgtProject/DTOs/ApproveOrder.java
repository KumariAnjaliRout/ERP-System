package com.InventoryMgt.InventoryMgtProject.DTOs;


import com.InventoryMgt.InventoryMgtProject.Entities.OrderApprovalAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApproveOrder {
    private Long orderId;
    private OrderApprovalAction action;
}
