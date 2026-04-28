package com.InventoryMgt.InventoryMgtProject.Config;

import com.InventoryMgt.InventoryMgtProject.DTOs.OutletListResponse;
import com.InventoryMgt.InventoryMgtProject.DTOs.OutletResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutletClientService {

    private final AuthFeignClient outletFeignClient;

    public List<OutletResponse> getOrganizationOutlets(String organizationId){

        try {

            List<OutletResponse> outlets =
                    outletFeignClient.getOutletsByOrganization(organizationId);

            return outlets != null ? outlets : List.of();

        } catch (Exception ex){

            log.error("Failed to fetch outlets for organization {}", organizationId, ex);
            return List.of();
        }
    }

    public List<OutletResponse> getAllOutlets(){

        try {

            List<OutletResponse> outlets =
                    outletFeignClient.getAllOutlets();

            return outlets != null ? outlets : List.of();

        } catch (Exception ex){

            log.error("Failed to fetch all outlets", ex);
            return List.of();
        }
    }

    public OutletResponse getOutletById(String outletId){

        try {

            return outletFeignClient.getOutletById(outletId);

        } catch (Exception ex){

            log.error("Failed to fetch outlet {}", outletId, ex);
            return null;
        }
    }
}