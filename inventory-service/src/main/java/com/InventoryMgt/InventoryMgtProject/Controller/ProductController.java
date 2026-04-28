package com.InventoryMgt.InventoryMgtProject.Controller;

import com.InventoryMgt.InventoryMgtProject.Config.CustomUserPrincipal;
import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.*;
import com.InventoryMgt.InventoryMgtProject.Service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    // ================= CREATE PRODUCT =================
    @PostMapping(
            value = "/add",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProductResponseDTO> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws IOException {
        ProductRequestDTO dto =
                objectMapper.readValue(productJson, ProductRequestDTO.class);
        //  VALIDATION TRIGGER
        Set<ConstraintViolation<ProductRequestDTO>> violations =
                validator.validate(dto);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        ProductResponseDTO response =
                productService.createProduct(
                        dto,
                        image,
                        principal.getOrganizationId()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================= GET PRODUCTS =================
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','OUTLET')")
    public ResponseEntity<List<ProductResponseDTO>> getProducts(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                productService.getProducts(
                        principal.getOrganizationId()
                )
        );
    }


    // ================= DELETE PRODUCT =================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal){

        productService.deleteProduct(
                id,
                principal.getOrganizationId()
        );

        return ResponseEntity.ok("Product deleted successfully");
    }

    // ================= UPDATE PRODUCT =================
    @PutMapping(
            value = "/update/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws IOException {

        ProductRequestDTO dto =
                objectMapper.readValue(productJson, ProductRequestDTO.class);

        return ResponseEntity.ok(
                productService.updateProduct(
                        id,
                        dto,
                        image,
                        principal.getOrganizationId()
                )
        );
    }


    // ================= PRODUCT STATUS =================
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ProductStatusResponseDTO getProductStats(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return productService.getProductStats(
                principal.getOrganizationId()
        );
    }

    @GetMapping("/stock-health")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<StockHealthStats>> getStockHealth(
            @AuthenticationPrincipal CustomUserPrincipal principal){

        return ResponseEntity.ok(
                productService.getStockHealth(
                        principal.getOrganizationId()
                )
        );
    }
}









