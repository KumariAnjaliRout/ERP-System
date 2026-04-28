package com.erp.accountantservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductStatusResponseDTO {
    private String status;
    private List<ProductStatusItemDTO> data;
}
