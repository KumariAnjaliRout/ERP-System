package com.InventoryMgt.InventoryMgtProject.Controller;

import com.InventoryMgt.InventoryMgtProject.Config.CustomUserPrincipal;
import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.ApiResponse;
import com.InventoryMgt.InventoryMgtProject.DTOs.WishlistResponse;
import com.InventoryMgt.InventoryMgtProject.Service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
@PreAuthorize("hasRole('OUTLET')")
public class WishlistController {

    private final WishlistService wishlistService;

    // GET WISHLIST
    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(
                wishlistService.getWishlist(principal.getOutletId())
        );
    }

    // ADD PRODUCT
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse> addToWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        wishlistService.addToWishlist(
                principal.getOutletId(),
                principal.getOrganizationId(),
                productId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Product added to wishlist"));
    }

    // REMOVE PRODUCT
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse> removeFromWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        wishlistService.removeFromWishlist(
                principal.getOutletId(),
                productId
        );

        return ResponseEntity.ok(
                new ApiResponse(true, "Product removed from wishlist")
        );
    }

    // CLEAR WISHLIST
    @DeleteMapping
    public ResponseEntity<ApiResponse> clearWishlist(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        wishlistService.clearWishlist(principal.getOutletId());

        return ResponseEntity.ok(
                new ApiResponse(true, "Wishlist cleared")
        );
    }
}

