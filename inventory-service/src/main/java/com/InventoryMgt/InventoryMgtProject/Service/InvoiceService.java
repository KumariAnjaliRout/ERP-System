package com.InventoryMgt.InventoryMgtProject.Service;

import com.InventoryMgt.InventoryMgtProject.DTOs.OrganizationResponse;
import com.InventoryMgt.InventoryMgtProject.DTOs.OutletResponse;
import com.InventoryMgt.InventoryMgtProject.Entities.Order;
import com.InventoryMgt.InventoryMgtProject.Entities.OrderItem;
import com.InventoryMgt.InventoryMgtProject.Expection.InvoiceGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final TemplateEngine templateEngine;

    public String generateHtml(
            Order order,
            OrganizationResponse organization,
            OutletResponse outlet,
            List<OrderItem> items,
            String invoiceNo
    ){

        try {

            // Subtotal (before discount)
            BigDecimal subtotal = items.stream()
                    .map(i -> BigDecimal.valueOf(i.getUnitPrice())
                            .multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            //Total Discount
            BigDecimal totalDiscount = items.stream()
                    .map(i -> BigDecimal.valueOf(i.getDiscountAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Total GST
            BigDecimal gstTotal = items.stream()
                    .map(i -> BigDecimal.valueOf(i.getTaxAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Final Total
            BigDecimal totalAmount = items.stream()
                    .map(i -> BigDecimal.valueOf(i.getTotalPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Rounding
            subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
            totalDiscount = totalDiscount.setScale(2, RoundingMode.HALF_UP);
            gstTotal = gstTotal.setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP); //set scale keeps 2 decimal places and roundindmode.half_up is like normal rounding if number greater than 0.5 we should add accordingly up

            Context context = new Context();

            context.setVariable("order", order);
            context.setVariable("organization", organization);
            context.setVariable("outlet", outlet);
            context.setVariable("items", items);

            context.setVariable("invoiceNo", invoiceNo);
            context.setVariable("invoiceDate", LocalDate.now());

            context.setVariable("subtotal", subtotal);
            context.setVariable("totalDiscount", totalDiscount);
            context.setVariable("gstTotal", gstTotal);
            context.setVariable("totalAmount", totalAmount);

            return templateEngine.process("invoice", context);

        } catch (Exception ex) {

            log.error("Invoice HTML generation failed for order {}", order.getId(), ex);
            throw new InvoiceGenerationException("Failed to generate invoice", ex);
        }
    }
}