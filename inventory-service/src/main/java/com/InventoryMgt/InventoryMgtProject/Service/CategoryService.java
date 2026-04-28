
package com.InventoryMgt.InventoryMgtProject.Service;

import com.InventoryMgt.InventoryMgtProject.Config.CustomUserPrincipal;
import com.InventoryMgt.InventoryMgtProject.Config.NotificationFeignClient;
import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.*;
import com.InventoryMgt.InventoryMgtProject.Entities.*;
import com.InventoryMgt.InventoryMgtProject.Expection.CategoryNotFoundException;
import com.InventoryMgt.InventoryMgtProject.Expection.DuplicateCategoryException;
import com.InventoryMgt.InventoryMgtProject.Expection.ImageUploadException;
import com.InventoryMgt.InventoryMgtProject.Repository.CategoryRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final NotificationFeignClient notificationFeignClient;
    private final S3Client s3Client;
    private final S3ServiceProduct s3ServiceProduct;

    private static final String CATEGORY_FOLDER = "categories";

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${cloud.aws.credentials.region}")
    private String region;


    // ================= CREATE CATEGORY =================

    public CategoryResponseDTO createCategory(CategoryRequestDTO request,
                                              MultipartFile image,
                                              String orgId,
                                              CustomUserPrincipal principal) throws IOException {

        validateName(request.getName(), "Category name");

        if (principal == null) {
            throw new AccessDeniedException("Unauthorized");
        }

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Category image is required");
        }

        if (categoryRepository.existsByNameAndOrganizationId(request.getName(), orgId)) {
            throw new DuplicateCategoryException("Category already exists");
        }

        String key = s3ServiceProduct.uploadFile(image, "categories");

        String imageUrl = uploadImage(image);

        Category category = Category.builder()
                .name(request.getName())
                .status(request.getStatus())
                .imageUrl(imageUrl)
                .organizationId(orgId)
                .createdBy(UUID.fromString(principal.getUserId()))   // FIXED
                .build();

        Category saved = categoryRepository.save(category);

        // Send notification AFTER DB success
        notificationFeignClient.sendNotification(
                NotificationRequestDto.builder()
                        .category(NotificationCategory.INVENTORY)
                        .type(NotificationType.CATEGORY_CREATED)
                        .priority(NotificationPriority.NORMAL)
                        .organizationId(orgId)
                        .metadata(Map.of(
                                "triggeredByRole", principal.getRole(),
                                "categoryId", category.getId(),
                                "categoryName", category.getName()
                        ))
                        .actionable(false)
                        .build()
        );

        return mapToResponse(saved);
    }


    // ================= GET ALL =================

    public List<CategoryResponseDTO> getAllCategories(String orgId) {

        return categoryRepository.findByOrganizationId(orgId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // ================= GET BY ID =================

    public CategoryResponseDTO getCategoryById(Long id, String orgId) {

        Category category = categoryRepository
                .findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        return mapToResponse(category);
    }


    // ================= UPDATE =================

    public CategoryResponseDTO updateCategory(Long id,
                                              CategoryRequestDTO request,
                                              MultipartFile image,
                                              String orgId) throws IOException {

        Category category = categoryRepository
                .findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        if (request.getName() != null) {
            validateName(request.getName(), "Category name");
        }

        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByNameAndOrganizationId(request.getName(), orgId)) {
            throw new DuplicateCategoryException("Category name already exists");
        }

        category.setName(request.getName());

        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        if (image != null && !image.isEmpty()) {

            // 1. Upload new image FIRST
            String newImageUrl = uploadImage(image);

            // 2. Delete old image AFTER success
            deleteImage(category.getImageUrl());

            // 3. Update DB
            category.setImageUrl(newImageUrl);
        }

        Category updated = categoryRepository.save(category);

        return mapToResponse(updated);
    }


    // ================= DELETE =================

    public void deleteCategory(Long id, String orgId) {

        Category category = categoryRepository
                .findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        //deleteImage(category.getImageUrl());
        //corrected one is down
        s3ServiceProduct.deleteFile(category.getImageUrl());

        categoryRepository.delete(category);
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


    // ================= CATEGORY WITH PRODUCTS =================

    public CategoryWithFullProductsDTO getCategoryWithProducts(Long categoryId, String orgId) {

        Category category = categoryRepository
                .findCategoriesWithProducts(orgId)
                .stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        return mapCategoryToDTO(category);
    }


    // ================= ALL CATEGORIES WITH PRODUCTS =================

    public List<CategoryWithFullProductsDTO> getAllCategoriesWithProducts(String orgId) {

        return categoryRepository
                .findCategoriesWithProducts(orgId)
                .stream()
                .map(this::mapCategoryToDTO)
                .toList();
    }


    // ================= MAPPERS =================

    private CategoryResponseDTO mapToResponse(Category category) {

        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .imageUrl(category.getImageUrl())
                .status(category.getStatus())
                .build();
    }


    private CategoryWithFullProductsDTO mapCategoryToDTO(Category category) {

        List<CategoryWithFullProductsDTO.ProductDTO> products =
                category.getProducts()
                        .stream()
                        .map(product -> new CategoryWithFullProductsDTO.ProductDTO(
                                product.getId(),
                                product.getName(),
                                product.getProductImage(),
                                product.getPrice(),
                                product.getDiscount(),
                                product.getTax(),
                                product.getQuantity(),
                                product.getTotalPrice(),
                                s3ServiceProduct.getFileUrl(product.getProductImage()),
                                product.getProductStatus()
                        ))
                        .toList();

        return new CategoryWithFullProductsDTO(
                category.getId(),
                category.getName(),
                products
        );
    }

    // ================= S3 =================

    private String uploadImage(MultipartFile image) {

        try {

            String safeFileName =
                    UUID.randomUUID() + "_" +
                            image.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]", "_");

            String key = CATEGORY_FOLDER + "/" + safeFileName;

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(image.getContentType())
                            .build(),
                    RequestBody.fromInputStream(image.getInputStream(), image.getSize())
            );

            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;

        }
        catch (Exception ex) {

            log.error("Failed to upload category image", ex);

            throw new ImageUploadException("Image upload failed", ex);
        }
    }


    private void deleteImage(String imageUrl) {

        if (imageUrl == null) return;

        try {

            String key = imageUrl.substring(imageUrl.indexOf(".amazonaws.com/") + 15);

            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );
        }
        catch (Exception ex) {

            log.warn("Failed to delete image from S3: {}", imageUrl);
        }
    }
}
