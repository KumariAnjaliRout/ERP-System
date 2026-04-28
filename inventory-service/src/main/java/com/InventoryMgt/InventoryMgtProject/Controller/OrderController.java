package com.InventoryMgt.InventoryMgtProject.Controller;


import com.InventoryMgt.InventoryMgtProject.Config.CustomUserPrincipal;
import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.*;
import com.InventoryMgt.InventoryMgtProject.Entities.Order;
import com.InventoryMgt.InventoryMgtProject.Service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // OUTLET ORDERS
    @GetMapping("/outlet/my-orders")
    @PreAuthorize("hasRole('OUTLET')")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getOrdersByOutlet(principal.getOutletId())
        );
    }

    // CREATE ORDER
    @PostMapping
    @PreAuthorize("hasRole('OUTLET')")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody OrderRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        OrderResponse response = orderService.createOrderFromCart(
                request.getCartId(),
                principal.getOutletId(),
                principal.getOrganizationId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ADMIN PENDING ORDERS
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getPendingOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal){

        return ResponseEntity.ok(
                orderService.getPendingOrders(principal.getOrganizationId())
        );
    }

    // ADMIN DECISION
    @PutMapping("/admin/{orderId}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> adminDecision(
            @PathVariable Long orderId,
            @RequestBody ApproveOrder request,
            @AuthenticationPrincipal CustomUserPrincipal principal){

        request.setOrderId(orderId);

        return ResponseEntity.ok(
                orderService.adminAction(
                        request,
                        principal.getOrganizationId()
                )
        );
    }

    @GetMapping("/not-pending")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<List<OrderResponse>> getNonPendingOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getOrganizationOrdersExceptPending(
                        principal.getOrganizationId()
                )
        );
    }

    @GetMapping("/approved")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<OrderResponse>> getApprovedOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getApprovedOrders(principal.getOrganizationId())
        );
    }

    @GetMapping("/rejected")
    @PreAuthorize(("hasAnyRole('ADMIN','OUTLET')"))
    public ResponseEntity<List<OrderResponse>> getRejectedOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal){
        return ResponseEntity.ok(
                orderService.getRejectedOrders(principal.getOrganizationId())
        );
    }

    @GetMapping("/organization/all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<List<OrderResponse>> getAllOrdersForOrganization(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getAllOrdersByOrganization(
                        principal.getOrganizationId()
                )
        );
    }

    // APPROVED ORDER DETAILS
    @GetMapping("/approved/{orderId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<OrderResponse> getApprovedOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getApprovedOrderForDispatch(
                        orderId,
                        principal.getOrganizationId()
                )
        );
    }

    // DISPATCH ORDER
    @PutMapping("/manager/dispatch/{orderId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<OrderResponse> dispatchOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.managerDispatch(
                        orderId,
                        principal.getOrganizationId()
                )
        );
    }

    // OUTLET DISPATCHED ORDERS
    @GetMapping("/outlet/dispatched")
    @PreAuthorize("hasRole('OUTLET')")
    public ResponseEntity<List<OrderResponse>> getOutletDispatchedOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getDispatchedOrdersForOutlet(
                        principal.getOutletId()
                )
        );
    }

    // DISPATCHED ORDERS
    @GetMapping("/manager/dispatched")
    @PreAuthorize("hasAnyRole('MANAGER','OUTLET')")
    public ResponseEntity<List<OrderResponse>> getDispatchedOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getDispatchedOrders(
                        principal.getOrganizationId()
                )
        );
    }

    // CONFIRM DELIVERY
    @PutMapping("/outlet/confirm-delivery/{orderId}")
    @PreAuthorize("hasRole('OUTLET')")
    public ResponseEntity<OrderResponse> confirmDelivery(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserPrincipal principal){

        return ResponseEntity.ok(
                orderService.confirmDelivery(
                        orderId,
                        principal.getOutletId()
                )
        );
    }

    // OUTLET DELIVERED ORDERS
    @GetMapping("/outlet/delivered")
    @PreAuthorize("hasAnyRole('OUTLET','MANAGER')")
    public ResponseEntity<List<OrderResponse>> getDeliveredOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal){

        return ResponseEntity.ok(
                orderService.getDeliveredOrders(
                        principal.getOutletId()
                )
        );
    }

    // DOWNLOAD INVOICE
    @GetMapping("/invoice/{orderId}")
    @PreAuthorize("hasAnyRole('OUTLET','MANAGER','ADMIN','ACCOUNTANT')")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserPrincipal principal){

        byte[] pdf =
                orderService.downloadInvoice(
                        orderId,
                        principal.getOutletId(),
                        principal.getOrganizationId(),
                        principal.getRole()
                );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice_"+orderId+".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL,"no-cache")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ACCOUNTANT & MANAGER DELIVERED ORDERS
    @GetMapping("/accountant/delivered")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','MANAGER')")
    public ResponseEntity<List<OrderResponse>> getDeliveredOrdersForAccountant(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getDeliveredOrdersForAccountant(
                        principal.getOrganizationId()
                )
        );
    }

    // OUTLET STATS-Top Outlets
    @GetMapping("/stats/outlets")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','SUPER_ACCOUNTANT')")
    public ResponseEntity<List<OutletOrderStatsResponse>> getOutletStats(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getOutletOrderStats(
                        principal.getOrganizationId()
                )
        );
    }

    // PRODUCT DEMAND-Top Products
    @GetMapping("/product-stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<List<ProductDemandStats>> getProductDemandStats(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                orderService.getProductDemandStats(
                        principal.getOrganizationId()
                )
        );
    }

    // RECENT ORDERS
    @GetMapping("/stats/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SUPER_ACCOUNTANT','ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<Page<RecentOrderSummary>> getRecentOrders(
            @RequestParam(required = false) Integer days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(
                page,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return ResponseEntity.ok(
                orderService.getRecentOrders(days, pageable)
        );
    }

    //top selling products in categories
    @GetMapping("/category-stats")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<CategoryStats>> getTopCategories(
            @AuthenticationPrincipal CustomUserPrincipal principal){

        return ResponseEntity.ok(
                orderService.getCategoryStats(
                        principal.getOrganizationId()
                )
        );
    }

    //monthly orders
    @GetMapping("/monthly-stats")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<MonthlyOrderStats>> getMonthlyOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal){

        return ResponseEntity.ok(
                orderService.getMonthlyOrders(
                        principal.getOrganizationId()
                )
        );
    }

    //top outlets
    @GetMapping("/outlets/top")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<OutletOrderStatsResponse>> getTopOutlets(
            @AuthenticationPrincipal CustomUserPrincipal principal){

        return ResponseEntity.ok(
                orderService.getOutletOrderStats(
                        principal.getOrganizationId()
                )
        );
    }

    // GLOBAL REVENUE
    @GetMapping("/stats/organizations/revenue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SUPER_ACCOUNTANT')")
    public ResponseEntity<List<OrganizationRevenueStats>> getOrganizationRevenueStats() {
        return ResponseEntity.ok(
                orderService.getGlobalOrganizationRevenueStats()
        );
    }
}
