package com.InventoryMgt.InventoryMgtProject.Service;

import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.CartItemResponse;
import com.InventoryMgt.InventoryMgtProject.DTOs.CartResponse;
import com.InventoryMgt.InventoryMgtProject.Entities.*;
import com.InventoryMgt.InventoryMgtProject.Expection.CartItemNotFoundException;
import com.InventoryMgt.InventoryMgtProject.Expection.CartNotFoundException;
import com.InventoryMgt.InventoryMgtProject.Expection.ProductNotFoundException;
import com.InventoryMgt.InventoryMgtProject.Expection.StockExceededException;
import com.InventoryMgt.InventoryMgtProject.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final S3ServiceProduct s3ServiceProduct;

    // ================= GET OR CREATE CART =================
    @Transactional
    public Cart getOrCreateCart(String outletId) {

        return cartRepository
                .findActiveCartWithItems(outletId)
                .orElseGet(() -> {

                    Cart cart = Cart.builder()
                            .outletId(outletId)
                            .status(CartStatus.ACTIVE)
                            .totalAmount(0.0)
                            .itemCount(0)
                            .build();

                    return cartRepository.save(cart);
                });
    }


    // ================= ADD TO CART =================

    @Transactional
    public Long addToCart(String outletId,
                          String organizationId,
                          Long productId,
                          int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Cart cart = getOrCreateCart(outletId);

        Product product = productRepository
                .findByIdAndOrganizationId(productId, organizationId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found"));

        if (product.getProductStatus() == ProductStatus.OUT) {
            throw new StockExceededException("Product out of stock");
        }

        if (quantity > product.getQuantity()) {
            throw new StockExceededException("Requested quantity exceeds available stock");
        }

        CartItems existingItem =
                cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                        .orElse(null);

        // ================= EXISTING ITEM =================
        if (existingItem != null) {

            int newQty = existingItem.getQuantity() + quantity;

            if (newQty > product.getQuantity()) {
                throw new StockExceededException("Stock exceeded");
            }

            existingItem.setQuantity(newQty);
            existingItem.setUnitPrice(product.getPrice());

            //  CALCULATION
            double base = product.getPrice() * newQty;

            double discountPercent = product.getDiscount();
            double taxPercent = product.getTax();

            double discountAmount = base * discountPercent / 100;
            double afterDiscount = base - discountAmount;

            double taxAmount = afterDiscount * taxPercent / 100;

            double finalPrice = afterDiscount + taxAmount;

            existingItem.setTotalPrice(finalPrice);

            cartItemRepository.save(existingItem);
            updateCartTotals(cart);

            return existingItem.getId();
        }

        // ================= NEW ITEM =================

        double base = product.getPrice() * quantity;

        double discountPercent = product.getDiscount();
        double taxPercent = product.getTax();

        double discountAmount = base * discountPercent / 100;
        double afterDiscount = base - discountAmount;

        double taxAmount = afterDiscount * taxPercent / 100;

        double finalPrice = afterDiscount + taxAmount;

        CartItems item = CartItems.builder()
                .cart(cart)
                .product(product)
                .productName(product.getName())
                .productImage(product.getProductImage())
                .unitPrice(product.getPrice())
                .quantity(quantity)
                .totalPrice(finalPrice)
                .build();

        cartItemRepository.save(item);

        updateCartTotals(cart);

        return item.getId();
    }

    @Transactional
    public void updateQuantity(String outletId,
                               Long cartItemId,
                               int newQuantity) {

        CartItems item =
                cartItemRepository.findByIdAndCartOutletId(cartItemId, outletId)
                        .orElseThrow(() ->
                                new CartItemNotFoundException("Cart item not found"));

        Product product = item.getProduct();

        if (newQuantity <= 0) {
            removeFromCart(outletId, cartItemId);
            return;
        }

        if (newQuantity > product.getQuantity()) {
            throw new StockExceededException("Insufficient stock");
        }

        item.setQuantity(newQuantity);
        item.setUnitPrice(product.getPrice());

        // ✅ CALCULATION
        double base = product.getPrice() * newQuantity;

        double discountPercent = product.getDiscount();
        double taxPercent = product.getTax();

        double discountAmount = base * discountPercent / 100;
        double afterDiscount = base - discountAmount;

        double taxAmount = afterDiscount * taxPercent / 100;

        double finalPrice = afterDiscount + taxAmount;

        item.setTotalPrice(finalPrice);

        cartItemRepository.save(item);

        updateCartTotals(item.getCart());
    }
//    @Transactional
//    public Long addToCart(String outletId,
//                          String organizationId,
//                          Long productId,
//                          int quantity) {
//
//        if (quantity <= 0) {
//            throw new IllegalArgumentException("Quantity must be greater than zero");
//        }
//
//        Cart cart = getOrCreateCart(outletId);
//
//        Product product = productRepository
//                .findByIdAndOrganizationId(productId, organizationId)
//                .orElseThrow(() ->
//                        new ProductNotFoundException("Product not found"));
//
//        if (product.getProductStatus() == ProductStatus.OUT) {
//            throw new StockExceededException("Product out of stock");
//        }
//
//        if (quantity > product.getQuantity()) {
//            throw new StockExceededException("Requested quantity exceeds available stock");
//        }
//
//        CartItems existingItem =
//                cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
//                        .orElse(null);
//
//        if (existingItem != null) {
//
//            int newQty = existingItem.getQuantity() + quantity;
//
//            if (newQty > product.getQuantity()) {
//                throw new StockExceededException("Stock exceeded");
//            }
//
//            existingItem.setQuantity(newQty);
//            existingItem.setUnitPrice(product.getPrice());
//            existingItem.setTotalPrice(product.getPrice() * newQty);
//
//            cartItemRepository.save(existingItem);
//            updateCartTotals(cart);
//
//            return existingItem.getId();
//        }
//
//        CartItems item = CartItems.builder()
//                .cart(cart)
//                .product(product)
//                .productName(product.getName())
//                .productImage(product.getProductImage())
//                .unitPrice(product.getPrice())
//                .quantity(quantity)
//                .totalPrice(product.getPrice() * quantity)
//                .build();
//
//        cartItemRepository.save(item);
//
//        updateCartTotals(cart);
//
//        return item.getId();
//    }
//
//    // ================= UPDATE QUANTITY =================
//
//    @Transactional
//    public void updateQuantity(String outletId,
//                               Long cartItemId,
//                               int newQuantity) {
//
//        CartItems item =
//                cartItemRepository.findByIdAndCartOutletId(cartItemId, outletId)
//                        .orElseThrow(() ->
//                                new CartItemNotFoundException("Cart item not found"));
//
//        Product product = item.getProduct();
//
//        if (newQuantity <= 0) {
//            removeFromCart(outletId, cartItemId);
//            return;
//        }
//
//        if (newQuantity > product.getQuantity()) {
//            throw new StockExceededException("Insufficient stock");
//        }
//
//        item.setQuantity(newQuantity);
//        item.setUnitPrice(product.getPrice());
//        item.setTotalPrice(product.getPrice() * newQuantity);
//
//        cartItemRepository.save(item);
//
//        updateCartTotals(item.getCart());
//    }

    // ================= REMOVE ITEM =================

    @Transactional
    public void removeFromCart(String outletId, Long cartItemId) {

        CartItems item =
                cartItemRepository.findByIdAndCartOutletId(cartItemId, outletId)
                        .orElseThrow(() ->
                                new CartItemNotFoundException("Cart item not found"));

        Cart cart = item.getCart();

        cart.getItems().remove(item);   // 🔥 this alone is enough

        updateCartTotals(cart);
    }


    // ================= CLEAR CART =================

    @Transactional
    public void clearCart(String outletId) {

        Cart cart = getOrCreateCart(outletId);

        cart.getItems().clear();   // 🔥 CORRECT

        cart.setTotalAmount(0.0);
        cart.setItemCount(0);
    }

    // ================= GET CART =================

    public CartResponse getCart(String outletId) {

        Cart cart = getOrCreateCart(outletId);

        return mapToCartResponse(cart);
    }


    // ================= CART ITEM COUNT =================

    public int getCartItemCount(String outletId) {

        return cartRepository
                .findActiveCartWithItems(outletId)
                .map(Cart::getItemCount)
                .orElse(0);
    }



    // ================= UPDATE TOTALS =================
    @Transactional
    protected void updateCartTotals(Cart cart) {

        List<CartItems> items =
                cartItemRepository.findByCartId(cart.getId());

        double totalAmount =
                items.stream()
                        .mapToDouble(CartItems::getTotalPrice)
                        .sum();

        int itemCount =
                items.stream()
                        .mapToInt(CartItems::getQuantity)
                        .sum();

        cart.setTotalAmount(totalAmount);
        cart.setItemCount(itemCount);

        cartRepository.save(cart);
    }

    // ================= DTO MAPPERS =================

    private CartResponse mapToCartResponse(Cart cart) {

        List<CartItemResponse> itemResponses =
                cart.getItems()
                        .stream()
                        .map(this::mapToCartItemResponse)
                        .toList();

        return new CartResponse(
                cart.getId(),
                cart.getOutletId(),
                cart.getTotalAmount(),
                cart.getItemCount(),
                itemResponses
        );
    }

    private CartItemResponse mapToCartItemResponse(CartItems item) {
       String key=item.getProductImage();
       String imageUrl=key!=null
               ?s3ServiceProduct.getFileUrl(key)
               :null;

        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getProductImage(),
                imageUrl,
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice()
        );
    }
}
