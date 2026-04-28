package com.InventoryMgt.InventoryMgtProject.Controller;

import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.AddToCartRequest;
import com.InventoryMgt.InventoryMgtProject.DTOs.UpdateCartItemRequest;
import com.InventoryMgt.InventoryMgtProject.DTOs.CartResponse;
import com.InventoryMgt.InventoryMgtProject.DTOs.CartItemResponse;
import com.InventoryMgt.InventoryMgtProject.Entities.*;
import com.InventoryMgt.InventoryMgtProject.Service.*;
import com.InventoryMgt.InventoryMgtProject.Config.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 1. GET CART
    @PreAuthorize("hasRole('OUTLET')")
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                cartService.getCart(principal.getOutletId())
        );
    }

    // 2. ADD TO CART

    @PreAuthorize("hasRole('OUTLET')")
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        Long cartItemId = cartService.addToCart(
                principal.getOutletId(),
                principal.getOrganizationId(),
                request.getProductId(),
                request.getQuantity()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Item added successfully",
                "cartItemId", cartItemId
        ));
    }

    // 3. UPDATE QUANTITY
    @PreAuthorize("hasRole('OUTLET')")
    @PutMapping("/update")
    public ResponseEntity<Map<String, String>> updateQuantity(
            @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        cartService.updateQuantity(
                principal.getOutletId(),
                request.getCartItemId(),
                request.getQuantity()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Cart updated successfully"
        ));
    }

    // 4. GET CART COUNT
    @PreAuthorize("hasRole('OUTLET')")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCartCount(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        int count = cartService.getCartItemCount(principal.getOutletId());

        return ResponseEntity.ok(Map.of(
                "itemCount", count
        ));
    }

    // 5. REMOVE ITEM
    @PreAuthorize("hasRole('OUTLET')")
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<Map<String, String>> removeFromCart(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        cartService.removeFromCart(principal.getOutletId(), cartItemId);

        return ResponseEntity.ok(Map.of(
                "message", "Item removed from cart"
        ));
    }

    // 6. CLEAR CART
    @PreAuthorize("hasRole('OUTLET')")
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCart(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        cartService.clearCart(principal.getOutletId());

        return ResponseEntity.ok(Map.of(
                "message", "Cart cleared successfully"
        ));
    }
}

