package com.erp.accountantservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OutletSummaryDTO {
//    private String outletId;
//    private String outletName;
//
//    // Financial fields
//    private BigDecimal expenses;
//    private BigDecimal revenue;
//    private BigDecimal profit;
//
//    // Stock fields
//    private Integer stockOrdered;
//    private BigDecimal costOfStock;
//
//    // Order fields
//    private LocalDate lastOrderDate;
//    private String lastOrderStatus;
//    private String lastOrderId;

    private String outletId;
    private String outletName;

    private BigDecimal revenue;
    private Integer stockOrdered;
}