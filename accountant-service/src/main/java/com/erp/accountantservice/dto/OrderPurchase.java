package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPurchase {

    private Long orderId;
    private String orderDate;
    private String orderStatus;
    private double orderTotal;
    private List<ProductItem> products;

}