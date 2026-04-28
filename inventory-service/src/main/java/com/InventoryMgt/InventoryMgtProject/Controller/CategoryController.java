package com.InventoryMgt.InventoryMgtProject.Controller;

import com.InventoryMgt.InventoryMgtProject.Config.CustomUserPrincipal;
import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.CategoryRequestDTO;
import com.InventoryMgt.InventoryMgtProject.DTOs.CategoryResponseDTO;
import com.InventoryMgt.InventoryMgtProject.DTOs.CategoryWithFullProductsDTO;
import com.InventoryMgt.InventoryMgtProject.Service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    // CREATE CATEGORY
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @RequestPart("category") String categoryJson,
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws IOException {
        CategoryRequestDTO request =
                objectMapper.readValue(categoryJson, CategoryRequestDTO.class);

        Set<ConstraintViolation<CategoryRequestDTO>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        return ResponseEntity.ok(
                categoryService.createCategory(
                        request,
                        image,
                        principal.getOrganizationId(),
                        principal
                )
        );
    }

    // GET ALL
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','OUTLET')")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                categoryService.getAllCategories(principal.getOrganizationId())
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','OUTLET')")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                categoryService.getCategoryById(id, principal.getOrganizationId())
        );
    }

    // UPDATE
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @RequestPart("category") String categoryJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws IOException {

        CategoryRequestDTO request =
                objectMapper.readValue(categoryJson, CategoryRequestDTO.class);

        return ResponseEntity.ok(
                categoryService.updateCategory(
                        id,
                        request,
                        image,
                        principal.getOrganizationId()
                )
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        categoryService.deleteCategory(id, principal.getOrganizationId());

        return ResponseEntity.ok("Category deleted successfully");
    }

    // CATEGORY WITH PRODUCTS
    @GetMapping("/{id}/products")
    @PreAuthorize("hasAnyRole('MANAGER','OUTLET')")
    public ResponseEntity<CategoryWithFullProductsDTO> getCategoryProducts(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                categoryService.getCategoryWithProducts(id, principal.getOrganizationId())
        );
    }

    // ALL CATEGORIES WITH PRODUCTS
    @GetMapping("/products/all")
    @PreAuthorize("hasAnyRole('MANAGER','OUTLET')")
    public ResponseEntity<List<CategoryWithFullProductsDTO>> getAllCategoriesWithProducts(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                categoryService.getAllCategoriesWithProducts(principal.getOrganizationId())
        );
    }
}
