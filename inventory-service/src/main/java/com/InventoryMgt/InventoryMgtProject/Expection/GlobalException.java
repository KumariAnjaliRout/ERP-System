package com.InventoryMgt.InventoryMgtProject.Expection;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import software.amazon.awssdk.core.exception.SdkException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalException {

    // ================= PRODUCT =================

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        return error(404, ex.getMessage());
    }

    @ExceptionHandler(DuplicateProductException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProduct(DuplicateProductException ex) {
        return error(409, ex.getMessage());
    }

    @ExceptionHandler(StockExceededException.class)
    public ResponseEntity<ErrorResponse> handleStockExceeded(StockExceededException ex) {
        return error(422, ex.getMessage());
    }

    // ================= CATEGORY =================

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryNotFoundException ex) {
        return error(404, ex.getMessage());
    }


    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCategory(DuplicateCategoryException ex) {
        return error(409, ex.getMessage());
    }

    // ================= CART =================

    // ================= CART =================

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCartItemNotFound(CartItemNotFoundException ex) {
        return error(404, ex.getMessage());
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCartNotFound(CartNotFoundException ex) {
        return error(404, ex.getMessage());
    }

    @ExceptionHandler(CartInactiveException.class)
    public ResponseEntity<ErrorResponse> handleCartInactive(CartInactiveException ex) {

        log.warn("Cart inactive: {}", ex.getMessage());

        return error(400, ex.getMessage());
    }
    // ================= WISHLIST =================

    @ExceptionHandler(DuplicateWishlistItemException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateWishlist(DuplicateWishlistItemException ex) {
        return error(409, ex.getMessage());
    }

    @ExceptionHandler(WishlistItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWishlistItemNotFound(WishlistItemNotFoundException ex) {
        return error(404, ex.getMessage());
    }

    // ================= INVOICE =================

    @ExceptionHandler(InvoiceGenerationException.class)
    public ResponseEntity<ErrorResponse> handleInvoiceGeneration(InvoiceGenerationException ex) {
        log.error("Invoice generation failed", ex);
        return error(500, ex.getMessage());
    }

    @ExceptionHandler(InvoiceNotGeneratedException.class)
    public ResponseEntity<ErrorResponse> handleInvoiceNotGenerated(
            InvoiceNotGeneratedException ex){

        log.warn("Invoice not generated: {}", ex.getMessage());

        return error(400, ex.getMessage());
    }

    @ExceptionHandler(InvoiceUploadException.class)
    public ResponseEntity<ErrorResponse> handleInvoiceUpload(InvoiceUploadException ex) {
        log.error("Invoice upload failed", ex);
        return error(500, ex.getMessage());
    }
    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderState(
            InvalidOrderStateException ex) {

        log.warn("Invalid order state: {}", ex.getMessage());

        return error(400, ex.getMessage());
    }

    // ================= FILE STORAGE =================

    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<ErrorResponse> handleImageUpload(ImageUploadException ex) {
        log.error("Image upload failed", ex);
        return error(500, ex.getMessage());
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException ex) {
        log.error("File storage error", ex);
        return error(500, ex.getMessage());
    }

    // ================= SECURITY =================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {

        log.warn("Access denied: {}", ex.getMessage());

        return error(403, "Access denied: insufficient permissions");
    }

    // ================= VALIDATION =================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");

        return error(400, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {

        log.warn("Invalid request: {}", ex.getMessage());

        return error(400, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex) {

        String message = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst()
                .orElse("Validation error");

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .status(400)
                        .error(message)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // ================= FEIGN =================

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {

        log.error("Feign error status={} message={}", ex.status(), ex.getMessage());

        return switch (ex.status()) {

            case 400 -> error(400, "Bad request to external service");

            case 401 -> error(401, "Unauthorized request to external service");

            case 403 -> error(403, "Access denied by external service");

            case 404 -> error(404, "External service resource not found");

            default -> error(503, "External service unavailable");
        };
    }

    // ================= AWS / S3 =================

    @ExceptionHandler(SdkException.class)
    public ResponseEntity<ErrorResponse> handleAwsError(SdkException ex) {

        log.error("AWS service error", ex);

        return error(503, "File storage service unavailable");
    }

    // ================= FILE UPLOAD =================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSize(MaxUploadSizeExceededException ex) {

        return error(413, "File size exceeds allowed limit");
    }

    // ================= DATABASE =================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseError(DataIntegrityViolationException ex) {

        String message = "Database constraint violation";

        if (ex.getMessage().contains("uk_product_name_org")) {
            message = "Product with this name already exists in this organization";
        }

        if (ex.getMessage().contains("uk_category_name_org")) {
            message = "Category with this name already exists";
        }

        return error(409, message);
    }

    // ================= JSON =================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {

        return error(400, "Malformed JSON request body");
    }

    // ================= HTTP =================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {

        return error(405, "HTTP method not supported for this endpoint");
    }

    // ================= FALLBACK =================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {

        log.error("Unhandled exception", ex);

        return error(500, "Internal Server Error");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {

        log.warn("Unauthorized access: {}", ex.getMessage());

        return error(401, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {

        log.warn("Unauthorized access: {}", ex.getMessage());

        return error(404, ex.getMessage());
    }
    // ================= RESPONSE BUILDER =================
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {

        log.warn("Invalid endpoint requested: {}", ex.getResourcePath());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Endpoint not found");
        response.put("path", ex.getResourcePath());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handleNoHandler(NoHandlerFoundException ex) {

        log.warn("Invalid API endpoint called: {}", ex.getRequestURL());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "API endpoint not found",
                        "path", ex.getRequestURL()
                ));
    }

    private ResponseEntity<ErrorResponse> error(int status, String message) {

        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .status(status)
                        .error(message)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

}

