package com.erp.accountantservice.client;

import com.erp.accountantservice.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "OrderService", url = "http://localhost:8083")
public interface OrderProductApiClient {

    @GetMapping("/api/orders/stats/recent")
    Page<RecentOrderSummary> getRecentOrders(
            @RequestParam(required = false) Integer days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/api/orders/invoice/{orderId}")
    ResponseEntity<byte[]> downloadInvoice(
            @PathVariable("orderId") Long orderId
    );
}