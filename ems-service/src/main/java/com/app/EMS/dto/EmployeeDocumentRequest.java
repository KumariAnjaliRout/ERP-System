package com.app.EMS.dto;
import lombok.*;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDocumentRequest {
    private String documentType;
    private String documentNumber;
}
