package com.erp.erpsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletResponse {
    private String id;
    private String name;
    private String organizationId;
    private UUID outletOwnerId;
    private Boolean isActive;
    private String address;
}