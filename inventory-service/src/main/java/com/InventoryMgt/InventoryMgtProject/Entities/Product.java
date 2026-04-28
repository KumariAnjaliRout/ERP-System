package com.InventoryMgt.InventoryMgtProject.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_org", columnList = "organization_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name","organization_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String productImage;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private double discount;

    @Column(nullable = false)
    private double tax;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock", nullable = false)
    private ProductStatus productStatus;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    //for locking
    @Version
    private Long version;
}