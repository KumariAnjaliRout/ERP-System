package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatusDTO {
    private Integer totalProducts;
    private Integer lowStock;
    private Integer outOfStock;
    private Integer inStock;
}
