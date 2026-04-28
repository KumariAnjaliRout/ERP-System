package com.InventoryMgt.InventoryMgtProject.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"cart_id","product_id"})
        },
        indexes = {
                @Index(name = "idx_cart_item_cart", columnList = "cart_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id",nullable=false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id",nullable=false)
    private Product product;

    private String productName;

    private String productImage;

    private Integer quantity;

    private Double unitPrice;

    private Double totalPrice;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

//    @PrePersist
//    @PreUpdate
//    public void calculateTotalPrice() {
//        if (quantity != null && unitPrice != null) {
//            this.totalPrice = quantity * unitPrice;
//        }
//    }
}