package com.app.EMS.dto;
import com.app.EMS.entity.Roles;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class EmployeeDocumentResponse {
    private Long id;
    private String employeeId;
    private String documentType;
    private String documentNumber;
    private String fileName;
    private String filePath;
    private LocalDateTime uploadedAt;
    private Roles role;
    private String fileHash;
}
