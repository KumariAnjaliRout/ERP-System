package com.erp.accountantservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private String invoiceNumber;
    private String outletName;
    private String invoiceDate;
    private List<InvoiceItemDTO> items;
    private BigDecimal totalAmount;

}