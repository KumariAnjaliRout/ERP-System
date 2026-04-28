package com.InventoryMgt.InventoryMgtProject.reporting;

import com.InventoryMgt.InventoryMgtProject.Config.AuthFeignClient;
import com.InventoryMgt.InventoryMgtProject.Config.OutletClientService;
import com.InventoryMgt.InventoryMgtProject.DTOs.*;
import com.InventoryMgt.InventoryMgtProject.Entities.Order;
import com.InventoryMgt.InventoryMgtProject.Entities.OrderStatus;
import com.InventoryMgt.InventoryMgtProject.Repository.OrderRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private final OrderRepository orderRepository;
    private final AuthFeignClient authFeignClient;
    private final OutletClientService outletClientService;

    // ORGANIZATION PURCHASE REPORT
    public OutletPurchaseReport getOrganizationOutletPurchases(String organizationId, Integer days) {

        try {

            log.info("Generating outlet purchase report for org {}", organizationId);

            days = (days == null || days <= 0) ? 7 : days;

            Instant start = LocalDate.now()
                    .minusDays(days)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

            Instant end = LocalDate.now()
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();

            List<OutletResponse> outlets =
                    outletClientService.getOrganizationOutlets(organizationId);

            if (outlets.isEmpty()) {
                log.warn("No outlets found for organization {}", organizationId);
                return emptyReport(organizationId);
            }

            Map<String,String> outletNames =
                    outlets.stream()
                            .collect(Collectors.toMap(
                                    OutletResponse::getId,
                                    OutletResponse::getName,
                                    (a,b)->a
                            ));

            List<String> outletIds =
                    outlets.stream()
                            .map(OutletResponse::getId)
                            .toList();

            String orgName = fetchOrganizationName(organizationId);

            List<Order> orders =
                    orderRepository
                            .findByOrganizationIdAndOutletIdInAndOrderStatusAndCreatedAtBetween(
                                    organizationId,
                                    outletIds,
                                    OrderStatus.DELIVERED,
                                    start,
                                    end
                            );

            log.info("Orders fetched for org {} : {}", organizationId, orders.size());

            return buildReport(
                    organizationId,
                    orgName,
                    orders,
                    outletNames
            );

        } catch (Exception ex) {

            log.error("Unexpected error while generating organization report", ex);
            throw new RuntimeException("Failed to generate organization report");
        }
    }


    // GLOBAL PURCHASE REPORT
    public List<OutletPurchaseReport> getAllOrganizationsOutletPurchases(Integer days) {

        try {

            days = (days == null || days <= 0) ? 7 : days;

            Instant start = LocalDate.now()
                    .minusDays(days)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();

            Instant end = LocalDate.now()
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();

            List<OutletResponse> outlets = fetchAllOutlets();

            if(outlets.isEmpty()){
                return List.of();
            }

            Map<String,List<OutletResponse>> outletsByOrg =
                    outlets.stream()
                            .collect(Collectors.groupingBy(
                                    OutletResponse::getOrganizationId
                            ));

            List<OutletPurchaseReport> reports = new ArrayList<>();

            for(String orgId : outletsByOrg.keySet()){

                List<OutletResponse> orgOutlets = outletsByOrg.get(orgId);

                Map<String,String> outletNames =
                        orgOutlets.stream()
                                .collect(Collectors.toMap(
                                        OutletResponse::getId,
                                        OutletResponse::getName,
                                        (a,b)->a
                                ));

                List<String> outletIds =
                        orgOutlets.stream()
                                .map(OutletResponse::getId)
                                .toList();

                String orgName = fetchOrganizationName(orgId);

                List<Order> orders =
                        orderRepository
                                .findByOrganizationIdAndOutletIdInAndOrderStatusAndCreatedAtBetween(
                                        orgId,
                                        outletIds,
                                        OrderStatus.DELIVERED,
                                        start,
                                        end
                                );

                reports.add(
                        buildReport(
                                orgId,
                                orgName,
                                orders,
                                outletNames
                        )
                );
            }

            return reports;

        } catch (Exception ex) {

            log.error("Unexpected error while generating global report", ex);
            throw new RuntimeException("Failed to generate reports");
        }
    }


    // FETCH ORGANIZATION OUTLETS
    private List<OutletResponse> fetchOrganizationOutlets(String organizationId){

        try{

            return authFeignClient.getOutletsByOrganization(organizationId);

        }catch (Exception ex){

            log.error("Failed to fetch outlets for org {}", organizationId, ex);
            return List.of();
        }
    }


    // FETCH ALL OUTLETS
    private List<OutletResponse> fetchAllOutlets(){

        try{

            return authFeignClient.getAllOutlets();

        }catch (Exception ex){

            log.error("Failed to fetch outlets", ex);
            return List.of();
        }
    }

    // FETCH ORGANIZATION NAME
    private String fetchOrganizationName(String orgId){

        try{
            OrganizationResponse organization =
                    authFeignClient.getOrganizationById(orgId);

            return organization != null
                    ? organization.getName()
                    : "Unknown Organization";

        }catch (FeignException.NotFound ex){

            log.warn("Organization {} not found", orgId);
            return "Unknown Organization";

        }catch (Exception ex){

            log.warn("Failed to fetch organization {}", orgId);
            return "Unknown Organization";
        }
    }


    // BUILD REPORT
    private OutletPurchaseReport buildReport(
            String organizationId,
            String organizationName,
            List<Order> orders,
            Map<String,String> outletNames
    ){

        Map<String, OutletPurchaseReport.OutletData> outletMap = new HashMap<>();

        BigDecimal orgRevenue = BigDecimal.ZERO;

        for (String outletId : outletNames.keySet()) {

            outletMap.put(
                    outletId,
                    OutletPurchaseReport.OutletData.builder()
                            .outletId(outletId)
                            .outletName(outletNames.get(outletId))
                            .totalOrders(0L)
                            .revenue(BigDecimal.ZERO)
                            .build()
            );
        }

        for (Order order : orders) {

            String outletId = order.getOutletId();

            OutletPurchaseReport.OutletData outlet =
                    outletMap.get(outletId);

            if (outlet == null) continue;

            BigDecimal amount =
                    order.getTotalAmount() == null
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(order.getTotalAmount());

            outlet.setTotalOrders(
                    outlet.getTotalOrders() + 1
            );

            outlet.setRevenue(
                    outlet.getRevenue().add(amount)
            );

            orgRevenue = orgRevenue.add(amount);
        }

        return OutletPurchaseReport.builder()
                .organizationId(organizationId)
                .organizationName(organizationName)
                .totalOutlets(outletMap.size())
                .organizationRevenue(orgRevenue)
                .outlets(new ArrayList<>(outletMap.values()))
                .build();
    }


    // EMPTY REPORT
    private OutletPurchaseReport emptyReport(String orgId){

        return OutletPurchaseReport.builder()
                .organizationId(orgId)
                .organizationName("Unknown Organization")
                .totalOutlets(0)
                .organizationRevenue(BigDecimal.ZERO)
                .outlets(List.of())
                .build();
    }
}