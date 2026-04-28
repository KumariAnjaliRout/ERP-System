package com.InventoryMgt.InventoryMgtProject.Service;

import com.InventoryMgt.InventoryMgtProject.Config.AuthFeignClient;
import com.InventoryMgt.InventoryMgtProject.Config.NotificationFeignClient;
import com.InventoryMgt.InventoryMgtProject.Config.OutletClientService;
import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.*;
import com.InventoryMgt.InventoryMgtProject.Entities.*;
import com.InventoryMgt.InventoryMgtProject.Expection.*;
import com.InventoryMgt.InventoryMgtProject.Repository.*;

import com.InventoryMgt.InventoryMgtProject.util.PdfGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URL;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InvoiceService invoiceService;
    private final AuthFeignClient authFeignClient;
    private final NotificationFeignClient notificationFeignClient;
    private final PdfGenerator pdfGenerator;
    private final S3InvoiceService s3InvoiceService;
    private final OutletClientService outletClientService;
    private final S3ServiceProduct s3ServiceProduct;

    @Transactional
    public OrderResponse createOrderFromCart(Long cartId,
                                             String outletId,
                                             String organizationId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() ->
                        new CartNotFoundException("Cart not found"));

        if (!cart.getOutletId().equals(outletId))
            throw new CartNotFoundException("Cart does not belong to this outlet");

        if (cart.getStatus() != CartStatus.ACTIVE)
            throw new CartInactiveException("Cart is not active");

        if (cart.getItems().isEmpty())
            throw new CartInactiveException("Cart is empty");

        if (orderRepository.existsByCartId(cartId))
            throw new InvalidOrderStateException("Order already exists");

        Order order = Order.builder()
                .cartId(cartId)
                .outletId(outletId)
                .organizationId(organizationId)
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(0.0)
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = new ArrayList<>();

        for (CartItems cartItem : cart.getItems()) {

            Product product = productRepository
                    .findProductForUpdate(
                            cartItem.getProduct().getId(),
                            organizationId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getQuantity() < cartItem.getQuantity())
                throw new StockExceededException("Insufficient stock");

            // 🔹 CALCULATION (CORE LOGIC)
            double base = product.getPrice() * cartItem.getQuantity();

            double discountPercent = product.getDiscount();
            double taxPercent = product.getTax();

            double discountAmount = base * discountPercent / 100;
            double afterDiscount = base - discountAmount;

            double taxAmount = afterDiscount * taxPercent / 100;

            double finalPrice = afterDiscount + taxAmount;

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .productName(product.getName())
                    .productImage(product.getProductImage())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .discount(discountPercent)
                    .tax(taxPercent)
                    .discountAmount(discountAmount)
                    .taxAmount(taxAmount)
                    .totalPrice(finalPrice)
                    .build();

            items.add(orderItem);
        }

        orderItemRepository.saveAll(items);

        // 🔹 TOTAL
        double totalAmount = items.stream()
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();

        savedOrder.setTotalAmount(totalAmount);
        orderRepository.save(savedOrder);

        cart.setStatus(CartStatus.CONVERTED);
        cartRepository.save(cart);

        notificationFeignClient.sendNotification(
                NotificationRequestDto.builder()
                        .category(NotificationCategory.ORDER)
                        .type(NotificationType.ORDER_CREATED)
                        .priority(NotificationPriority.NORMAL)
                        .organizationId(savedOrder.getOrganizationId())
                        .metadata(Map.of(
                                "triggeredByRole", "ROLE_OUTLET",
                                "orderId", savedOrder.getId(),
                                "outletId", savedOrder.getOutletId()
                        ))
                        .actionable(true)
                        .build()
        );

        return buildOrderResponse(savedOrder);
    }

    //for getting orders by outlet
    public List<OrderResponse> getOrdersByOutlet(String outletId){

        List<Order> orders =
                orderRepository.findByOutletIdOrderByCreatedAtDesc(outletId);
        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    //for admin to view all pending orders
    public List<OrderResponse> getPendingOrders(String organizationId){
        List<Order> orders =
                orderRepository.findByOrganizationIdAndOrderStatus(
                        organizationId,
                        OrderStatus.PENDING
                );

        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    public List<OrderResponse> getAllOrdersByOrganization(String organizationId) {

        List<Order> orders =
                orderRepository.findByOrganizationIdOrderByCreatedAtDesc(
                        organizationId
                );

        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    public List<OrderResponse> getOrganizationOrdersExceptPending(String orgId) {
        List<Order> orders =
                orderRepository.findByOrganizationIdAndOrderStatusNot(
                        orgId,
                        OrderStatus.PENDING
                );
        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    @Transactional
    public OrderResponse adminAction(ApproveOrder request,
                                     String organizationId) {

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getOrganizationId().equals(organizationId))
            throw new RuntimeException("Invalid organization");

        if (order.getOrderStatus() != OrderStatus.PENDING) {

            log.warn("Admin attempted action on order {} with status {}",
                    order.getId(),
                    order.getOrderStatus());

            throw new InvalidOrderStateException(
                    "Order already processed. Current status: " + order.getOrderStatus()
            );
        }

//        order.setOrderStatus(OrderStatus.REJECTED);

        switch (request.getAction()) {

            case APPROVE -> order.setOrderStatus(OrderStatus.APPROVED);

            case REJECT -> order.setOrderStatus(OrderStatus.REJECTED);

            default -> throw new RuntimeException("Invalid action");
        }

        Order updatedOrder = orderRepository.save(order);

        if (updatedOrder.getOrderStatus() == OrderStatus.APPROVED) {

            notificationFeignClient.sendNotification(
                    NotificationRequestDto.builder()
                            .category(NotificationCategory.ORDER)
                            .type(NotificationType.ORDER_APPROVED)
                            .priority(NotificationPriority.HIGH)
                            .organizationId(updatedOrder.getOrganizationId())
                            .targetRole("MANAGER")
                            .metadata(Map.of(
                                    "triggeredByRole", "ROLE_ADMIN",
                                    "orderId", updatedOrder.getId(),
                                    "outletId", updatedOrder.getOutletId()
                            ))
                            .actionable(true)
                            .build()
            );
        }

        if (updatedOrder.getOrderStatus() == OrderStatus.REJECTED) {

            UUID outletUserId = resolveOutletUserId(order.getOutletId());

            notificationFeignClient.sendNotification(
                    NotificationRequestDto.builder()
                            .category(NotificationCategory.ORDER)
                            .type(NotificationType.ORDER_REJECTED)
                            .priority(NotificationPriority.NORMAL)
                            .organizationId(order.getOrganizationId())
                            .targetUserId(outletUserId)
                            .targetRole("OUTLET")
                            .metadata(Map.of(
                                    "triggeredByRole", "ROLE_ADMIN",
                                    "orderId", order.getId(),
                                    "outletId", order.getOutletId()
                            ))
                            .build()
            );
        }

        return buildOrderResponse(updatedOrder);
    }

    public OrderResponse getApprovedOrderForDispatch(Long orderId,
                                                     String organizationId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getOrganizationId().equals(organizationId))
            throw new RuntimeException("Invalid organization");

        if (order.getOrderStatus() != OrderStatus.APPROVED)
            throw new RuntimeException("Order not approved");

        return buildOrderResponse(order);
    }


    public List<OrderResponse> getApprovedOrders(String organizationId) {

        List<Order> orders =
                orderRepository.findByOrganizationIdAndOrderStatus(
                        organizationId,
                        OrderStatus.APPROVED
                );

        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    public List<OrderResponse> getRejectedOrders(String organizationId){
        List <Order> orders=orderRepository.findByOrganizationIdAndOrderStatus(
                organizationId,
                OrderStatus.REJECTED
        );
        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    @Transactional
    public OrderResponse managerDispatch(Long orderId,
                                         String organizationId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getOrganizationId().equals(organizationId))
            throw new RuntimeException("Invalid organization");

        if (order.getOrderStatus() != OrderStatus.APPROVED) {

            log.warn("Dispatch blocked for order {} with status {}",
                    order.getId(),
                    order.getOrderStatus());

            throw new InvalidOrderStateException(
                    "Order must be APPROVED before dispatch. Current status: "
                            + order.getOrderStatus()
            );
        }

        List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(orderId);

        List<Product> updatedProducts = new ArrayList<>();

        for (OrderItem item : items) {

            Product product = productRepository
                    .findProductForUpdate(
                            item.getProduct().getId(),
                            organizationId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            int newQty = product.getQuantity() - item.getQuantity();

            if (newQty < 0)
                throw new RuntimeException("Insufficient stock");

            product.setQuantity(newQty);

            updatedProducts.add(product);
        }

        productRepository.saveAll(updatedProducts);

        order.setOrderStatus(OrderStatus.DISPATCHED);

        Order updated = orderRepository.save(order);

        UUID outletUserId = resolveOutletUserId(updated.getOutletId());

        notificationFeignClient.sendNotification(
                NotificationRequestDto.builder()
                        .category(NotificationCategory.ORDER)
                        .type(NotificationType.ORDER_DISPATCHED)
                        .priority(NotificationPriority.HIGH)
                        .organizationId(updated.getOrganizationId())
                        .targetUserId(outletUserId)
                        .targetRole("OUTLET")
                        .metadata(Map.of(
                                "triggeredByRole", "ROLE_MANAGER",
                                "orderId", updated.getId(),
                                "outletId", updated.getOutletId()
                        ))
                        .build()
        );

        return buildOrderResponse(updated);
    }

    @Transactional
    public OrderResponse confirmDelivery(Long orderId, String outletId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getOutletId().equals(outletId)) {
            throw new UnauthorizedException("Unauthorized outlet");
        }

        if (order.getOrderStatus() != OrderStatus.DISPATCHED) {

            log.warn("Delivery confirmation blocked for order {} with status {}",
                    order.getId(),
                    order.getOrderStatus());

            throw new InvalidOrderStateException(
                    "Order must be DISPATCHED before delivery confirmation. Current status: "
                            + order.getOrderStatus()
            );
        }

        order.setOrderStatus(OrderStatus.DELIVERED);

        // ================= GENERATE INVOICE =================
        try {

            if (order.getInvoiceGenerated() == null || !order.getInvoiceGenerated()) {

                log.info("Generating invoice for order {}", orderId);

                List<OrderItem> items =
                        orderItemRepository.findByOrderIdWithProduct(orderId);

                OutletResponse outlet =
                        authFeignClient.getOutletById(order.getOutletId());

                OrganizationResponse organization =
                        authFeignClient.getOrganizationById(order.getOrganizationId());

                String invoiceNo =
                        "INV-" + Year.now().getValue() +
                                "-" + String.format("%06d", order.getId());

                String html =
                        invoiceService.generateHtml(
                                order,
                                organization,
                                outlet,
                                items,
                                invoiceNo
                        );

                byte[] pdf =
                        pdfGenerator.generatePdf(html);

                String invoiceUrl =
                        s3InvoiceService.uploadInvoice(pdf, invoiceNo);

                order.setInvoiceNumber(invoiceNo);
                // order.setInvoiceUrl(invoiceUrl);
                String key = s3InvoiceService.uploadInvoice(pdf, invoiceNo);

                order.setInvoiceUrl(key); // ✅ now it stores key
                order.setInvoiceGenerated(true);
            }

        } catch (Exception ex) {

            log.error("Invoice generation failed for order {}", orderId, ex);

            throw new InvoiceNotGeneratedException("Failed to generate invoice");
        }

        Order saved = orderRepository.save(order);

        // ================= SEND NOTIFICATION =================
        try {

            notificationFeignClient.sendNotification(
                    NotificationRequestDto.builder()
                            .category(NotificationCategory.ORDER)
                            .type(NotificationType.ORDER_DELIVERED)
                            .priority(NotificationPriority.NORMAL)
                            .organizationId(saved.getOrganizationId())
                            .targetRole("MANAGER")
                            .metadata(Map.of(
                                    "triggeredByRole", "ROLE_OUTLET",
                                    "orderId", saved.getId(),
                                    "outletId", saved.getOutletId()
                            ))
                            .actionable(false)
                            .build()
            );

        } catch (Exception ex) {
            log.warn("Notification failed but order processed");
        }

        return buildOrderResponse(saved);
    }

    //get delivered orders by outlet
    public List<OrderResponse> getDeliveredOrders(String outletId){

        List<Order> orders =
                orderRepository.findByOutletIdAndOrderStatus(
                        outletId,
                        OrderStatus.DELIVERED
                );

        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }
    public byte[] downloadInvoice(Long orderId,
                                  String outletId,
                                  String organizationId,
                                  String role) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getInvoiceUrl() == null) {

            log.warn("Invoice download attempted before generation for order {}", orderId);

            throw new InvoiceNotGeneratedException(
                    "Invoice not generated yet for this order"
            );
        }

        // Outlet restriction
        if ("ROLE_OUTLET".equals(role)) {

            if (!order.getOutletId().equals(outletId)) {
                throw new UnauthorizedException("Unauthorized access");
            }
        }

        // Organization roles
        if ("ROLE_ADMIN".equals(role) ||
                "ROLE_MANAGER".equals(role) ||
                "ROLE_ACCOUNTANT".equals(role)) {

            if (!order.getOrganizationId().equals(organizationId)) {
                throw new UnauthorizedException("Unauthorized organization");
            }
        }

        try {

//            URL url = new URL(order.getInvoiceUrl());
//            return url.openStream().readAllBytes();
            return s3InvoiceService.downloadInvoiceFromS3(order.getInvoiceUrl());

        } catch (Exception ex) {

            log.error("Invoice download failed for order {}", orderId, ex);
            throw new InvoiceNotGeneratedException("Failed to download invoice");
        }
    }


    public List<OrderResponse> getDeliveredOrdersForAccountant(String organizationId) {

        List<Order> orders =
                orderRepository
                        .findByOrganizationIdAndOrderStatusAndInvoiceGeneratedTrueOrderByCreatedAtDesc(
                                organizationId,
                                OrderStatus.DELIVERED
                        );

        if (orders.isEmpty()) {

            log.info("No delivered orders with invoices found for org {}", organizationId);

            return List.of();
        }

        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    //helper method to get outletowneruserId
    private UUID resolveOutletUserId(String outletId) {

        OutletResponse outlet =
                authFeignClient.getOutletById(outletId);

        if (outlet == null || outlet.getOutletOwnerId() == null) {
            throw new RuntimeException("Outlet user not found for outletId: " + outletId);
        }
        return outlet.getOutletOwnerId();
    }

    public List<OrderResponse> getDispatchedOrders(String organizationId) {

        List<Order> orders =
                orderRepository.findByOrganizationIdAndOrderStatus(
                        organizationId,
                        OrderStatus.DISPATCHED
                );

        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    public List<OrderResponse> getDispatchedOrdersForOutlet(String outletId) {

        List<Order> orders =
                orderRepository.findByOutletIdAndOrderStatus(
                        outletId,
                        OrderStatus.DISPATCHED
                );

        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    public List<OutletOrderStatsResponse> getOutletOrderStats(String organizationId) {

        try {

            List<Object[]> orderCounts =
                    orderRepository.findOrderCountsByOutlet(organizationId);

            if (orderCounts.isEmpty()) {
                log.info("No orders found for organization {}", organizationId);
                return List.of();
            }

            List<OutletResponse> outlets =
                    outletClientService.getOrganizationOutlets(organizationId);

            if(outlets.isEmpty()){
                log.warn("No outlets returned from auth for organization {}", organizationId);
                return List.of();
            }

            Map<String,String> outletNames =
                    outlets.stream()
                            .collect(Collectors.toMap(
                                    OutletResponse::getId,
                                    OutletResponse::getName,
                                    (a,b)->a
                            ));

            return orderCounts.stream()
                    .map(row -> {

                        String outletId = row[0].toString();
                        Long totalOrders = ((Number) row[1]).longValue();

                        return new OutletOrderStatsResponse(
                                outletId,
                                outletNames.getOrDefault(outletId, "Unknown Outlet"),
                                totalOrders
                        );
                    })
                    .toList();

        } catch (Exception ex) {

            log.error("Failed to generate outlet order stats for org {}", organizationId, ex);
            throw new RuntimeException("Failed to fetch outlet order stats");
        }
    }


    //get product stats
    public List<ProductDemandStats> getProductDemandStats(String organizationId) {

        try {

            List<Object[]> data =
                    orderItemRepository.findProductDemandStats(organizationId);

            if (data.isEmpty()) {

                log.info("No product demand stats found for org {}", organizationId);

                return List.of();
            }

            return data.stream()
                    .map(row -> new ProductDemandStats(
                            ((Number) row[0]).longValue(),
                            row[1].toString(),
                            ((Number) row[2]).longValue(),
                            ((Number) row[3]).intValue()
                    ))
                    .toList();

        } catch (Exception ex) {

            log.error("Failed to fetch product demand stats for org {}", organizationId, ex);

            throw new RuntimeException("Failed to fetch product demand stats");
        }
    }

    // ================= RECENT ORDERS =================
    public Page<RecentOrderSummary> getRecentOrders(Integer days, Pageable pageable) {

        String role = SecurityUtil.getCurrentRole();
        String orgId = SecurityUtil.getCurrentOrganizationId();
        // Handle null → default 5
        if (days == null) {
            days = 5;
        }
        //Negative → not allowed
        if (days < 0) {
            throw new IllegalArgumentException("Days cannot be negative");
        }

        // If 0 → return empty page
        if (days == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        Instant start = startDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant end = endDate
                .atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant();

        //  DB pagination happens HERE
        Page<Order> orders;

        if (role.equals("ROLE_SUPER_ADMIN") || role.equals("ROLE_SUPER_ACCOUNTANT")) {

            // see all orgs
            orders = orderRepository.findRecentOrders(start, end, pageable);

        } else {

            // filter by org
            orders = orderRepository.findRecentOrdersByOrg(
                    start,
                    end,
                    orgId,
                    pageable
            );
        }

        // handle empty case
        if (orders.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, orders.getTotalElements());
        }

        List<Long> orderIds = orders.getContent()
                .stream()
                .map(Order::getId)
                .toList();

        // batch fetch (avoids N+1)
        List<OrderItem> items =
                orderItemRepository.findByOrderIdIn(orderIds);

        // aggregate in memory (fast for small page size)
        Map<Long, Integer> productCounts =
                items.stream()
                        .collect(Collectors.groupingBy(
                                i -> i.getOrder().getId(),
                                Collectors.summingInt(OrderItem::getQuantity)
                        ));

        // map to DTO
        List<RecentOrderSummary> summaries =
                orders.getContent()
                        .stream()
                        .map(order -> new RecentOrderSummary(
                                order.getId(),
                                order.getOutletId(),
                                BigDecimal.valueOf(order.getTotalAmount()),
                                order.getOrderStatus().name(),
                                order.getCreatedAt(),
                                productCounts.getOrDefault(order.getId(), 0)
                        ))
                        .toList();

        return new PageImpl<>(
                summaries,
                pageable,
                orders.getTotalElements()
        );
    }

    // ================= GLOBAL ORGANIZATION REVENUE STATS =================
    public List<OrganizationRevenueStats> getGlobalOrganizationRevenueStats() {

        List<Object[]> data =
                orderRepository.findOrganizationRevenueStats();

        return data.stream()
                .map(row -> {

                    String orgId = row[0].toString();
                    Long totalOrders = ((Number) row[1]).longValue();
                    BigDecimal revenue =
                            BigDecimal.valueOf(((Number) row[2]).doubleValue());

                    OrganizationResponse org =
                            authFeignClient.getOrganizationById(orgId);

                    String orgName =
                            org != null ? org.getName() : "Unknown";

                    return new OrganizationRevenueStats(
                            orgId,
                            orgName,
                            totalOrders,
                            revenue
                    );

                })
                .toList();
    }

    //Manager Dashboard stats
    public List<MonthlyOrderStats> getMonthlyOrders(String orgId) {

        return orderRepository.getOrdersByMonth(orgId)
                .stream()
                .map(r -> new MonthlyOrderStats(
                        ((Number) r[0]).intValue(),
                        ((Number) r[1]).intValue(),
                        ((Number) r[2]).longValue()
                ))
                .toList();
    }

    public List<CategoryStats> getCategoryStats(String orgId) {

        return orderItemRepository.getCategoryStats(orgId)
                .stream()
                .map(r -> new CategoryStats(
                        ((Number) r[0]).longValue(),
                        r[1].toString(),
                        ((Number) r[2]).longValue()
                ))
                .toList();
    }



    private OrderResponse buildOrderResponse(Order order) {

        List<OrderItem> items =
                orderItemRepository.findByOrderIdWithProduct(order.getId());

        List<OrderItemDTO> itemDTOs =
                items.stream()
                        .map(item -> {

                            String key = item.getProductImage();

                            String imageUrl = key != null
                                    ? s3ServiceProduct.getFileUrl(key)
                                    : null;

                            return OrderItemDTO.builder()
                                    .productId(item.getProduct().getId())
                                    .productName(item.getProductName())
                                    .productImage(key)
                                    .imageUrl(imageUrl)
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .discount(item.getDiscount())
                                    .tax(item.getTax())
                                    .discountAmount(item.getDiscountAmount())
                                    .taxAmount(item.getTaxAmount())
                                    .totalPrice(item.getTotalPrice())
                                    .build();
                        })
                        .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .organizationId(order.getOrganizationId())
                .cartId(order.getCartId())
                .outletId(order.getOutletId())
                .orderStatus(OrderStatus.valueOf(order.getOrderStatus().name()))
                .createdAt(order.getCreatedAt())
                .invoiceNumber(order.getInvoiceNumber())
                .invoiceUrl(order.getInvoiceUrl())
                .invoiceGenerated(order.getInvoiceGenerated())
                .items(itemDTOs)
                .totalAmount(order.getTotalAmount())
                .message("Order " + order.getOrderStatus().name().toLowerCase())
                .build();
    }
}