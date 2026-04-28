package com.erp.accountantservice.dto;

import lombok.Data;

@Data
public class Period {
    private String from;
    private String to;
    private int days;
}