package com.erp.accountantservice.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outlets")
@Data
public class Outlet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;  // Which org this outlet belongs to

    @Column(name = "accountant_id")
    private String accountantId;

    private String address;
    private String city;
    private String phone;
    private String email;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}