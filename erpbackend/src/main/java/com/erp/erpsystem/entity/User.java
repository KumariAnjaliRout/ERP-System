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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "outlet_id")
    private String outletId;

    @Builder.Default
    @Column(name = "is_active", columnDefinition = "boolean default true")
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now();}

    public void updateEmail(String email) { this.email = email;}

    public void updateUsername(String username) { this.username = username;}

    public void updateRole(Role role) {
        this.role = role;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void assignToOutlet(String outletId) {
        this.outletId = outletId;
    }

    public void removeFromOutlet() {
        this.outletId = null;
    }
}