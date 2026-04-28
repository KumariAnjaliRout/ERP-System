package com.erp.accountantservice.client;

import com.erp.accountantservice.dto.OutletPurchaseReport;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@FeignClient(name = "InventoryReportService", url = "http://localhost:8083")
public interface OutletPurchaseFeignClient {

    @GetMapping("/api/reports/outlet-purchases")
    OutletPurchaseReport getOrganizationOutletPurchases(
            @RequestParam(required = false) Integer days
    );

    @GetMapping("/api/reports/global/outlet-purchases")
    List<OutletPurchaseReport> getAllOrganizationsOutletPurchases(
            @RequestParam(required = false) Integer days
    );
}




//@FeignClient(name = "InventoryReportService", url = "http://192.168.0.126:8083")
//public interface OutletPurchaseFeignClient {
//
//    @GetMapping("/api/reports/outlet-purchases")
//    OutletPurchaseReport getOrganizationOutletPurchases(
//            @RequestParam(required = false) Integer days
//    );
//
//
//    @GetMapping("/api/reports/global/outlet-purchases")
//    List<OutletPurchaseReport> getAllOrganizationsOutletPurchases(
//            @RequestHeader("Authorization") String token,
//            @RequestParam(required = false) Integer days
//    );
//
//
//}