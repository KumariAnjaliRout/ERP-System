package com.app.EMS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many docs per employee
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;
    @Column(unique = true,nullable = true, length = 50)
    private String documentNumber;
    @Column(unique = true,nullable = true, length = 50)
    private String fileName;
    private String filePath;
    @Column(name = "file_hash", unique = true)
    private String fileHash;

    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    private Roles role;

    @PrePersist
    public void onUpload() {
        this.uploadedAt = LocalDateTime.now();
    }
}
