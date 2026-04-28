package com.app.EMS.controller;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.EmployeeDocumentResponse;
import com.app.EMS.entity.DocumentType;
import com.app.EMS.exception.ResourceNotFoundException;
import com.app.EMS.service.EmployeeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController

@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class EmployeeDocumentController {

    private final EmployeeDocumentService documentService;
    // 🔹 Upload document
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @PostMapping(value="/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam DocumentType documentType,
            @RequestParam(required = false) String documentNumber,
            @RequestParam MultipartFile file
    ) throws IOException {
        documentService.uploadDocument(principal, documentType,documentNumber,file);
        return ResponseEntity.ok("Document uploaded successfully");
    }

    // 🔹 View employee documents
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping("/employee")
    public ResponseEntity<?> getEmployeeDocuments(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                documentService.getEmployeeDocuments(principal)
        );
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeeDocumentsbyhr(@PathVariable String employeeId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                documentService.getEmployeeDocumentsbyhr(employeeId,principal)
        );
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws IOException {

        File file = documentService.getDocumentFile(documentId, principal);

        if (!file.exists()) {
            throw new ResourceNotFoundException("File not found");
        }

        Path path = file.toPath();
        Resource resource = new UrlResource(path.toUri());

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<EmployeeDocumentResponse>> getAllDocuments(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(documentService.getAllDocuments(principal));
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ADMIN','SUPER_ADMIN','ACCOUNTANT','SUPER_ACCOUNTANT')")
    @GetMapping("/view/{documentId}")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws IOException {

        File file = documentService.getDocumentFile(documentId, principal);

        Resource resource = new UrlResource(file.toURI());

        String contentType = Files.probeContentType(file.toPath());

        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @PutMapping(value="/update/{documentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value="documentNumber", required=false) String documentNumber,
            @RequestParam(value="file", required=false) MultipartFile file
    ) throws IOException {

        documentService.updateDocument(
                documentId,
                principal,
                documentType,
                documentNumber,
                file
        );

        return ResponseEntity.ok("Document updated successfully");
    }
}
