package com.erp.accountantservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InvoiceItemDTO {
    private String name;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
}