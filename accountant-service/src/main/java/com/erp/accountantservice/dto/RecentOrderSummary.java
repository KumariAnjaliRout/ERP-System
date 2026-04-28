package com.erp.accountantservice.dto;
//
//import lombok.Data;
//import lombok.Builder;
//import java.time.LocalDate;
//import java.util.List;
//
//@Data
//@Builder
//public class RecentOrderSummaryDTO {
//    private Long id;
//    private String outletName;
//    private double orderTotal;
//    private String orderStatus;
//    private String orderDate;
//    private int numProducts;
//
//}
//
//
//
//package com.InventoryMgt.InventoryMgtProject.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;


//Used by Manager / Admin / Accountant dashboards.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecentOrderSummary {

    private Long orderId;
    private String outletId;
    private BigDecimal orderTotal;
    private String orderStatus;
    private Instant createdAt;
    private Integer totalProducts;

}