package com.InventoryMgt.InventoryMgtProject.DTOs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutletResponse {
    private String id;
    private String name;
    private String address;
    private String organizationId;
    private UUID outletOwnerId;
    private Boolean isActive;
}