package com.InventoryMgt.InventoryMgtProject.DTOs;


import com.InventoryMgt.InventoryMgtProject.Entities.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private Long cartId;
    private String outletId;
    private OrderStatus orderStatus;
    private String organizationId;
    private Instant createdAt;
    private List<OrderItemDTO> items;
    private Double totalAmount;
    private String message;

    //for invoice
    private String invoiceNumber;
    private String invoiceUrl;
    private Boolean invoiceGenerated;

}
