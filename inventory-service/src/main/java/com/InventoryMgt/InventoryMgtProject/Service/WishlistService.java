package com.InventoryMgt.InventoryMgtProject.Service;

import com.InventoryMgt.InventoryMgtProject.Config.CustomUserPrincipal;
import com.InventoryMgt.InventoryMgtProject.Config.SecurityUtil;
import com.InventoryMgt.InventoryMgtProject.DTOs.WishlistItemResponse;
import com.InventoryMgt.InventoryMgtProject.DTOs.WishlistResponse;
import com.InventoryMgt.InventoryMgtProject.Entities.WishList;
import com.InventoryMgt.InventoryMgtProject.Entities.WishlistItems;
import com.InventoryMgt.InventoryMgtProject.Entities.Product;
import com.InventoryMgt.InventoryMgtProject.Expection.DuplicateWishlistItemException;
import com.InventoryMgt.InventoryMgtProject.Expection.ProductNotFoundException;
import com.InventoryMgt.InventoryMgtProject.Expection.WishlistItemNotFoundException;
import com.InventoryMgt.InventoryMgtProject.Repository.WishlistRepository;
import com.InventoryMgt.InventoryMgtProject.Repository.WishlistItemsRepository;
import com.InventoryMgt.InventoryMgtProject.Repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemsRepository wishlistItemsRepository;
    private final ProductRepository productRepository;
    private final S3ServiceProduct s3ServiceProduct;

    // ================= GET WISHLIST =================
    @Transactional
    public WishlistResponse getWishlist(String outletId){

        WishList wishlist = getOrCreateWishlist(outletId);

        log.debug("Fetching wishlist for outlet {}", outletId);
        List<WishlistItems> items =
                wishlistItemsRepository.findByWishlistIdWithProduct(
                        wishlist.getId()
                );

        return convertToResponse(wishlist, items);
    }

    // ================= ADD PRODUCT =================
    @Transactional
    public void addToWishlist(String outletId,
                              String orgId,
                              Long productId){

        WishList wishlist = getOrCreateWishlist(outletId);

        if (wishlistItemsRepository.existsByWishlistIdAndProductId(
                wishlist.getId(), productId)) {

            throw new DuplicateWishlistItemException(
                    "Product already exists in wishlist"
            );
        }

        Product product =
                productRepository.findByIdAndOrganizationId(productId, orgId)
                        .orElseThrow(() ->
                                new ProductNotFoundException("Product not found"));

        WishlistItems item = WishlistItems.builder()
                .wishlist(wishlist)
                .product(product)
                .build();

        wishlistItemsRepository.save(item);

        updateCount(wishlist);
    }

    // ================= REMOVE PRODUCT =================
    @Transactional
    public void removeFromWishlist(String outletId, Long productId){

        WishList wishlist = getOrCreateWishlist(outletId);

        WishlistItems item =
                wishlistItemsRepository
                        .findByWishlistIdAndProductId(wishlist.getId(), productId)
                        .orElseThrow(() ->
                                new WishlistItemNotFoundException(
                                        "Product not found in wishlist"
                                ));

        wishlistItemsRepository.delete(item);

        updateCount(wishlist);
    }

    // ================= CLEAR WISHLIST =================
    @Transactional
    public void clearWishlist(String outletId){

        WishList wishlist = getOrCreateWishlist(outletId);

        wishlistItemsRepository.deleteByWishlistId(wishlist.getId());

        updateCount(wishlist);
    }

    // ================= INTERNAL METHODS =================

    @Transactional
    public WishList getOrCreateWishlist(String outletId){

        return wishlistRepository.findByOutletId(outletId)
                .orElseGet(() -> {

                    WishList wishlist = WishList.builder()
                            .outletId(outletId)
                            .itemCount(0)
                            .build();

                    return wishlistRepository.save(wishlist);
                });
    }

    private void updateCount(WishList wishlist){

        int count = (int) wishlistItemsRepository.countByWishlistId(
                wishlist.getId()
        );

        wishlist.setItemCount(count);

        wishlistRepository.save(wishlist);
    }

    private WishlistResponse convertToResponse(
            WishList wishlist,
            List<WishlistItems> items){

        WishlistResponse response = new WishlistResponse();

        response.setWishlistId(wishlist.getId());
        response.setItemCount(wishlist.getItemCount());

        response.setItems(
                items.stream()
                        .map(this::convertToItemResponse)
                        .toList()
        );

        return response;
    }

    private WishlistItemResponse convertToItemResponse(WishlistItems item){

        WishlistItemResponse response = new WishlistItemResponse();

        Product product = item.getProduct();
        response.setId(item.getId());

        response.setProductId(product != null ? product.getId() : null);
        response.setProductName(product != null ? product.getName() : "Product removed");
        String key=product!=null?product.getProductImage():null;
        response.setProductImage(key);
        response.setImageUrl(key!=null? s3ServiceProduct.getFileUrl(key):null);
        response.setPrice(product != null ? product.getPrice() : null);
        response.setAddedAt(item.getAddedAt());

        return response;
    }
}