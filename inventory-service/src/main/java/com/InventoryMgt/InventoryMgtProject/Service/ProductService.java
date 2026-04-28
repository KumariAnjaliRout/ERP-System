package com.InventoryMgt.InventoryMgtProject.Service;

import com.InventoryMgt.InventoryMgtProject.Config.NotificationFeignClient;
import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.*;
import com.InventoryMgt.InventoryMgtProject.Entities.*;
import com.InventoryMgt.InventoryMgtProject.Expection.CategoryNotFoundException;
import com.InventoryMgt.InventoryMgtProject.Expection.DuplicateProductException;
import com.InventoryMgt.InventoryMgtProject.Expection.ImageUploadException;
import com.InventoryMgt.InventoryMgtProject.Expection.ProductNotFoundException;
import com.InventoryMgt.InventoryMgtProject.Repository.CategoryRepository;
import com.InventoryMgt.InventoryMgtProject.Repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final S3ServiceProduct s3serviceProduct;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationFeignClient notificationFeignClient;

    private static final String PRODUCT_FOLDER = "products";


    // ================= CREATE PRODUCT =================

    public ProductResponseDTO createProduct(ProductRequestDTO dto,
                                            MultipartFile image,
                                            String orgId) {

        //  VALIDATION FIRST
        validateName(dto.getName(), "Product name");
        Category category = categoryRepository
                .findByIdAndOrganizationId(dto.getCategoryId(), orgId)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        if (productRepository.existsByNameAndOrganizationId(dto.getName(), orgId)) {
            throw new DuplicateProductException("Product already exists");
        }

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Product image is mandatory");
        }

        if (dto.getPrice() == null || dto.getPrice() <= 0) {
            throw new IllegalArgumentException("Product price must be greater than zero");
        }

        if (dto.getDiscount() != null && dto.getDiscount() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }

        if (dto.getTax() != null && dto.getTax() < 0) {
            throw new IllegalArgumentException("Tax cannot be negative");
        }

        String imageUrl = uploadProductImage(image);

        int quantity = dto.getQuantity() != null ? dto.getQuantity() : 0;

        ProductStatus status =
                quantity <= 0 ? ProductStatus.OUT : ProductStatus.IN;

        double discountPercent = dto.getDiscount() != null ? dto.getDiscount() : 0.0;
        double taxPercent = dto.getTax() != null ? dto.getTax() : 0.0;

        if (discountPercent > 100) {
            throw new IllegalArgumentException("Discount cannot exceed 100%");
        }

        double base = dto.getPrice();

        double discountAmount = base * discountPercent / 100;
        double afterDiscount = base - discountAmount;

        double taxAmount = afterDiscount * taxPercent / 100;

        double totalPrice = afterDiscount + taxAmount;


        //String key = s3Service.uploadFile(image, "products");

        Product product = Product.builder()
                .name(dto.getName())
                .productImage(imageUrl)
                .price(dto.getPrice())
                .category(category)
                .organizationId(orgId)
                .quantity(quantity)
                .productStatus(status)
                .discount(discountPercent)
                .tax(taxPercent)
                .totalPrice(totalPrice)
                .build();

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }


    // ================= GET PRODUCTS =================

    public List<ProductResponseDTO> getProducts(String orgId) {

        return productRepository.findProductsWithCategory(orgId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // ================= DELETE PRODUCT =================

    public void deleteProduct(Long productId, String orgId) {

        Product product =
                productRepository
                        .findByIdAndOrganizationId(productId, orgId)
                        .orElseThrow(() ->
                                new ProductNotFoundException("Product not found"));
        s3serviceProduct.deleteFile(product.getProductImage());

        try {
            productRepository.delete(product);
        }
        catch (DataIntegrityViolationException ex) {

            log.error("Product cannot be deleted due to references", ex);

            throw new IllegalStateException(
                    "Product cannot be deleted because it is used in orders");
        }
    }

    public ProductResponseDTO updateProduct(Long productId,
                                            ProductRequestDTO requestDTO,
                                            MultipartFile image,
                                            String orgId) {

        Product product = productRepository
                .findByIdAndOrganizationId(productId, orgId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found"));

        ProductStatus previousStatus = product.getProductStatus();
        Double previousPrice = product.getPrice();

        product.setName(requestDTO.getName());
        product.setPrice(requestDTO.getPrice());

        if (requestDTO.getName() != null &&
                !requestDTO.getName().equals(product.getName()) &&
                productRepository.existsByNameAndOrganizationId(requestDTO.getName(), orgId)) {

            throw new DuplicateProductException("Product with this name already exists");
        }

        if (requestDTO.getCategoryId() != null) {

            Category category = categoryRepository
                    .findByIdAndOrganizationId(requestDTO.getCategoryId(), orgId)
                    .orElseThrow(() ->
                            new CategoryNotFoundException("Category not found"));

            product.setCategory(category);
        }

        if (requestDTO.getName() != null) {
            validateName(requestDTO.getName(), "Product name");
            if (!requestDTO.getName().equals(product.getName()) &&
                    productRepository.existsByNameAndOrganizationId(requestDTO.getName(), orgId)) {
                throw new DuplicateProductException("Product with this name already exists");
            }
            product.setName(requestDTO.getName());
        }

        //  IMAGE UPDATE LOGIC
        if (image != null && !image.isEmpty()) {

            // 1. Upload NEW image first
            String newKey = s3serviceProduct.uploadFile(image, "products");

            // 2. Delete OLD image
            s3serviceProduct.deleteFile(product.getProductImage());

            // 3. Set new key
            product.setProductImage(newKey);
        }


        if (requestDTO.getPrice() != null && requestDTO.getPrice() > 0) {
            product.setPrice(requestDTO.getPrice());
        }

        if (requestDTO.getQuantity() != null) {

            product.setQuantity(requestDTO.getQuantity());

            product.setProductStatus(
                    requestDTO.getQuantity() <= 0 ?
                            ProductStatus.OUT :
                            ProductStatus.IN
            );
        }

        double discountPercent =
                requestDTO.getDiscount() != null ?
                        requestDTO.getDiscount() : product.getDiscount();

        double taxPercent =
                requestDTO.getTax() != null ?
                        requestDTO.getTax() : product.getTax();

        // VALIDATION (optional but good)
        if (discountPercent > 100) {
            throw new IllegalArgumentException("Discount cannot exceed 100%");
        }

        // CORRECT CALCULATION (PERCENTAGE)
        double base = product.getPrice();

        double discountAmount = base * discountPercent / 100;
        double afterDiscount = base - discountAmount;

        double taxAmount = afterDiscount * taxPercent / 100;

        double totalPrice = afterDiscount + taxAmount;

       // SET VALUES
        product.setDiscount(discountPercent);
        product.setTax(taxPercent);
        product.setTotalPrice(totalPrice);

        Product updatedProduct = productRepository.save(product);

        ProductStatus newStatus = updatedProduct.getProductStatus();

        /* ================= OUT OF STOCK ================= */

        if (previousStatus == ProductStatus.IN && newStatus == ProductStatus.OUT) {

            notificationFeignClient.sendNotification(
                    NotificationRequestDto.builder()
                            .category(NotificationCategory.INVENTORY)
                            .type(NotificationType.PRODUCT_OUT_OF_STOCK)
                            .priority(NotificationPriority.HIGH)
                            .organizationId(orgId)
                            .targetRole("MANAGER")
                            .metadata(Map.of(
                                    "triggeredByRole", "ROLE_MANAGER",
                                    "productId", updatedProduct.getId(),
                                    "productName", updatedProduct.getName()
                            ))
                            .build()
            );
        }

        /* ================= BACK IN STOCK ================= */

        if (previousStatus == ProductStatus.OUT && newStatus == ProductStatus.IN) {

            notificationFeignClient.sendNotification(
                    NotificationRequestDto.builder()
                            .category(NotificationCategory.INVENTORY)
                            .type(NotificationType.PRODUCT_BACK_IN_STOCK)
                            .priority(NotificationPriority.NORMAL)
                            .organizationId(orgId)
                            .targetRole("OUTLET")
                            .metadata(Map.of(
                                    "triggeredByRole", "ROLE_MANAGER",
                                    "productId", updatedProduct.getId(),
                                    "productName", updatedProduct.getName()
                            ))
                            .build()
            );
        }

        return mapToResponse(updatedProduct);
    }

    private void validateName(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }

        if (!name.matches("^[A-Za-z0-9 ]{2,50}$")) {
            throw new IllegalArgumentException(
                    field + " must be 2-50 chars, only letters, numbers and spaces"
            );
        }
    }



    // ================= PRODUCT STATS =================

    public ProductStatusResponseDTO getProductStats(String orgId) {

        long totalProducts = productRepository.countByOrganizationId(orgId);
        long lowStock = productRepository.countLowStockProducts(orgId);
        long outOfStock = productRepository.countOutOfStockProducts(orgId);

        long inStock = totalProducts - lowStock - outOfStock;

        return new ProductStatusResponseDTO(
                (int) totalProducts,
                (int) inStock,
                (int) lowStock,
                (int) outOfStock
        );
    }

    // ================= IMAGE UPLOAD =================

    private String uploadProductImage(MultipartFile image) {

        try {

            return s3serviceProduct.uploadFile(image, PRODUCT_FOLDER);

        }
        catch (Exception ex) {

            log.error("Product image upload failed", ex);

            throw new ImageUploadException("Failed to upload product image", ex);
        }
    }

    //product stats
    public List<StockHealthStats> getStockHealth(String orgId) {

        return productRepository.getStockData(orgId)
                .stream()
                .map(r -> {

                    int qty = ((Number) r[2]).intValue();

                    String status =
                            qty == 0 ? "OUT_OF_STOCK" :
                                    qty < 10 ? "LOW_STOCK" :
                                            "IN_STOCK";

                    return new StockHealthStats(
                            ((Number) r[0]).longValue(),
                            r[1].toString(),
                            qty,
                            status
                    );
                })
                .toList();
    }


    // ================= MAPPER =================

    private ProductResponseDTO mapToResponse(Product product) {

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .productImage(product.getProductImage())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .tax(product.getTax())
                .discount(product.getDiscount())
                .totalPrice(product.getTotalPrice())
                .imageUrl(s3serviceProduct.getFileUrl(product.getProductImage()))
                .productStatus(product.getProductStatus())
                .categoryId(product.getCategory().getId())
                .build();
    }
}
