package com.erp.erpsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "outlets")
public class Outlet {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "outlet_owner_id", columnDefinition = "uuid")
    private UUID outletOwnerId;

    @Builder.Default
    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive = true;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void assignOwner(UUID ownerId) {
        this.outletOwnerId = ownerId;
    }

    public void toggleActive(boolean active) {
        this.isActive = active;
    }

     public void updateName(String name) {
        this.name = name;
    }
    public void updateAddress(String address) {
        this.address = address;
    }
}