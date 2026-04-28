package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatusItemDTO {
    private Integer totalProducts;
    private String label;
    private Integer value;
}
