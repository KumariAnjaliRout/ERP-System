package com.app.EMS.service;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.EmployeeDocumentResponse;
import com.app.EMS.entity.*;
import com.app.EMS.exception.*;
import com.app.EMS.repository.EmployeeDocumentRepository;
import com.app.EMS.repository.EmployeePersonalDetailsRepository;
import com.app.EMS.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeDocumentService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final EmployeeRepository employeeRepository;
    private final EmployeePersonalDetailsRepository employeePersonalDetailsRepository;
    private final EmployeeDocumentRepository documentRepository;
    private void validateHierarchyAccess(CustomUserPrincipal principal, Employee targetEmployee) {

        String loggedRole = principal.getRole();
        UUID loggedUserId=UUID.fromString(principal.getUserId());
        Roles targetRole = targetEmployee.getRole();
        UUID targetUserId=targetEmployee.getUserId();

        // SUPER ADMIN → unrestricted
        if (loggedRole.equals("ROLE_SUPER_ADMIN")) {
            return;
        }

        // ADMIN → HR, MANAGER, ACCOUNTANT
        if (loggedRole.equals("ROLE_ADMIN")) {
            if (loggedUserId.equals(targetUserId)) {
                return;
            }
            if (targetRole == Roles.ROLE_HR ||
                    targetRole == Roles.ROLE_MANAGER ||
                    targetRole == Roles.ROLE_ACCOUNTANT ||
                    targetRole == Roles.ROLE_EMPLOYEE ) {
                return;
            }
            throw new ForbiddenException("Admin can view only HR, MANAGER, ACCOUNTANT and EMPLOYEE");
        }

        // HR → EMPLOYEE
        if (loggedRole.equals("ROLE_HR")) {
            if (loggedUserId.equals(targetUserId)) {
                return;
            }
            if (targetRole == Roles.ROLE_EMPLOYEE ) {
                return;
            }
            throw new ForbiddenException("HR can view only EMPLOYEE");
        }

        // Others → only own data
        UUID userId = UUID.fromString(principal.getUserId());

        if (!targetEmployee.getUserId().equals(userId)) {
            throw new ForbiddenException("You can access only your documents");
        }
    }
    private String generateFileHash(MultipartFile file) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(file.getBytes());

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                String s = Integer.toHexString(0xff & b);
                if (s.length() == 1) hex.append('0');
                hex.append(s);
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Unable to generate file hash");
        }
    }
    public void uploadDocument(
            CustomUserPrincipal principal,
            DocumentType documentType,
            String documentNumber,
            MultipartFile file
    ) throws IOException {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_HR")
                && !principal.getRole().equals("ROLE_MANAGER") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        String role=principal.getRole();

        Employee employee = employeeRepository
                .findByUserId(userid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User profile not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot upload your document...You are inactive user");
        EmployeePersonalDetails personal=employeePersonalDetailsRepository.findByEmployeeUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User personal details must be filled before uploading documents"));
        // 🔥 Prevent duplicate document type upload
        boolean exists = documentRepository
                .existsByEmployee_UserIdAndDocumentType(
                        userid,
                        documentType
                );
        String contentType = file.getContentType();

        if (contentType == null ||
                (!contentType.equals("application/pdf") &&
                        !contentType.startsWith("image/"))) {

            throw new BadRequestException("Only PDF and image files are allowed");
        }

        if (documentType != DocumentType.OTHER) {

            if (exists) {
                throw new AlreadyExistsResourceException(
                        documentType + " already uploaded. You cannot upload it twice."
                );
            }
        }

        if (documentType != DocumentType.PHOTO && (documentType != DocumentType.OTHER) &&
                (documentNumber == null || documentNumber.isBlank())) {

            throw new BadRequestException("Document number is required for " + documentType);
        }
        if (documentNumber != null &&
                documentRepository.existsByDocumentNumber(documentNumber)) {

            throw new AlreadyExistsResourceException(
                    "Document number already exists"
            );
        }
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new BadRequestException("File size exceeds limit (5MB)");
        }
        // 3️⃣ Generate file hash
        String fileHash = generateFileHash(file);

        if (documentRepository.existsByFileHash(fileHash))
            throw new AlreadyExistsResourceException(
                    "This document file already exists");

        // Create directory: uploads/ACS123/
        Path employeeDir = Paths.get(uploadDir,userid.toString());
        Files.createDirectories(employeeDir);


        // File name: AADHAR_originalname.pdf
        String fileName = documentType + "_" + file.getOriginalFilename();
        Path filePath = employeeDir.resolve(fileName);

        // Save file
//        Files.copy(file.getInputStream(), filePath);
        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (FileAlreadyExistsException ex) {
            throw new BadRequestException("Document already uploaded for type " + documentType);
        }

        // Save DB record
        EmployeeDocument document = EmployeeDocument.builder()
                .employee(employee)
                .documentType(documentType)
                .documentNumber(documentNumber)
                .fileName(fileName)
                .filePath(filePath.toString())
                .fileHash(fileHash)
                .role(Roles.valueOf(role))
                .build();
        try {
            documentRepository.save(document);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new AlreadyExistsResourceException(
                    "Document number already exists"
            );
        }
    }
    /* ========== GET DOCUMENTS ========== */

    public List<EmployeeDocumentResponse> getEmployeeDocuments(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_HR")
                && !principal.getRole().equals("ROLE_MANAGER") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid = UUID.fromString(principal.getUserId());
        Employee employee = employeeRepository
                .findByUserId(userid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User profile not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get your profile.You are inactive user");


        return documentRepository.findByEmployee_UserId(userid)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public List<EmployeeDocumentResponse> getEmployeeDocumentsbyhr(String employeeId,CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot see another user details");
        }
//        String role = principal.getRole();
        Employee target = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = target.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot get user documents in another organization");
            }
        }
        validateHierarchyAccess(principal, target);

        return documentRepository.findByEmployee_EmployeeId(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    /* ========== DOWNLOAD DOCUMENT ========== */

    public File getDocumentFile(Long documentId,CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_EMPLOYEE")
                && !principal.getRole().equals("ROLE_MANAGER") &&!principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")
        ) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid = UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employee = employeeRepository
                    .findByUserId(userid)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User profile not found"));
            boolean inactive = employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
            if (inactive)
                throw new BadRequestException("You cannot get your profile.You are inactive user");
        }
        EmployeeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        Employee employee = doc.getEmployee();
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot get user documents in another organization");
            }
        }

//        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")
//        ) {
//            EmployeeDocument doc_ = documentRepository
//                    .findByIdAndEmployee_UserId(documentId, userid)
//                    .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
//        }
        validateHierarchyAccess(principal, doc.getEmployee());
        return new File(doc.getFilePath());
    }

    private EmployeeDocumentResponse mapToResponse(EmployeeDocument doc) {
        return EmployeeDocumentResponse.builder()
                .id(doc.getId())
                .employeeId(doc.getEmployee().getEmployeeId())
                .documentType(String.valueOf(doc.getDocumentType()))
                .documentNumber(doc.getDocumentNumber())
                .fileName(doc.getFileName())
                .filePath(doc.getFilePath())
                .fileHash(doc.getFileHash())
                .uploadedAt(doc.getUploadedAt())
                .role(Roles.valueOf(String.valueOf(doc.getRole())))
                .build();
    }
    public List<EmployeeDocumentResponse> getAllDocuments(CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot see another user details");
        }
        String role = principal.getRole();
        String orgId=principal.getOrganizationId();

        // SUPER ADMIN → all
//        if (role.equals("ROLE_SUPER_ADMIN")) {
//            return documentRepository.findAll()
//                    .stream()
//                    .map(this::mapToResponse)
//                    .toList();
//        }
//
//        // ADMIN → HR, MANAGER, ACCOUNTANT
//        if (role.equals("ROLE_ADMIN")) {
//
//            List<Employee> employees = employeeRepository.findByRoleIn(
//                    List.of(
//                            "ROLE_HR",
//                            "ROLE_MANAGER",
//                            "ROLE_ACCOUNTANT",
//                            "ROLE_EMPLOYEE"
//                    )
//            );
//
//            List<String> ids = employees.stream()
//                    .map(Employee::getEmployeeId)
//                    .toList();
//
//            return documentRepository.findByEmployee_EmployeeIdIn(ids)
//                    .stream()
//                    .map(this::mapToResponse)
//                    .toList();
//        }
//
//        // HR → EMPLOYEE
//        if (role.equals("ROLE_HR")) {
//
//            List<Employee> employees = employeeRepository.findByRole(Roles.ROLE_EMPLOYEE);
//
//            List<String> ids = employees.stream()
//                    .map(Employee::getEmployeeId)
//                    .toList();
//
//            return documentRepository.findByEmployee_EmployeeIdIn(ids)
//                    .stream()
//                    .map(this::mapToResponse)
//                    .toList();
//        }
        // ✅ SUPER ADMIN → all documents (no org filter)
        if (role.equals("ROLE_SUPER_ADMIN")) {
            return documentRepository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        List<Employee> employees;

        // ✅ ADMIN → same org + limited roles
        if (role.equals("ROLE_ADMIN")) {

            employees = employeeRepository.findByOrganisationAndRoleIn(
                    orgId,
                    List.of(
                            "ROLE_HR",
                            "ROLE_MANAGER",
                            "ROLE_ACCOUNTANT",
                            "ROLE_EMPLOYEE"
                    )
            );
        }

        // ✅ HR → same org + only employees
        else if (role.equals("ROLE_HR")) {

            employees = employeeRepository.findByOrganisationAndRole(
                    orgId,
                    Roles.ROLE_EMPLOYEE
            );
        }

        else {
            throw new ForbiddenException("Access denied");
        }

        // Extract employeeIds
        List<String> ids = employees.stream()
                .map(Employee::getEmployeeId)
                .toList();

        return documentRepository.findByEmployee_EmployeeIdIn(ids)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    //    public void updateDocument(
//            Long documentId,
//            CustomUserPrincipal principal,
//            DocumentType documentType,
//            String documentNumber,
//            MultipartFile file
//    ) throws IOException {
//
//        if (principal == null)
//            throw new UnauthorizedException("Unauthorized");
//
//        String role = principal.getRole();
//        UUID userId = UUID.fromString(principal.getUserId());
//
//        if (!role.equals("ROLE_EMPLOYEE")
//                && !role.equals("ROLE_HR")
//                && !role.equals("ROLE_MANAGER")
//                && !role.equals("ROLE_ACCOUNTANT")
//                && !role.equals("ROLE_ADMIN")
//                && !role.equals("ROLE_SUPER_ACCOUNTANT")
//             )
//            throw new ForbiddenException("Access denied");
//
//
//        EmployeeDocument doc = documentRepository.findById(documentId)
//                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
//        EmployeeDocument doc_ = documentRepository
//                .findByIdAndEmployee_UserId(documentId, userId)
//                .orElseThrow(() -> new ResourceNotFoundException("Other Document cannot be updated"));
//
//        /* ================= VALIDATE DOC NUMBER ================= */
//
//        if (documentType != DocumentType.PHOTO &&
//                documentType != DocumentType.OTHER &&
//                (documentNumber == null || documentNumber.isBlank()))
//        {
//            throw new BadRequestException("Document number required for " + documentType);
//        }
//
//
//        /* ================= UPDATE FILE ================= */
//
//        if(file != null && !file.isEmpty()){
//
//            Path path = Paths.get(doc.getFilePath());
//
//            Files.copy(
//                    file.getInputStream(),
//                    path,
//                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
//            );
//
//            doc.setFileName(path.getFileName().toString());
//            doc.setFilePath(path.toString());
//        }
//
//
//        /* ================= UPDATE META ================= */
//
//        doc.setDocumentType(documentType);
//        doc.setDocumentNumber(documentNumber);
//        documentRepository.save(doc);
//    }
    public void updateDocument(
            Long documentId,
            CustomUserPrincipal principal,
            DocumentType documentType,
            String documentNumber,
            MultipartFile file
    ) throws IOException {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");

        String role = principal.getRole();
        UUID userId = UUID.fromString(principal.getUserId());

        if (!role.equals("ROLE_EMPLOYEE")
                && !role.equals("ROLE_HR")
                && !role.equals("ROLE_MANAGER")
                && !role.equals("ROLE_ACCOUNTANT")
                && !role.equals("ROLE_ADMIN")
                && !role.equals("ROLE_SUPER_ACCOUNTANT")
                && !role.equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }

        // ✅ Fetch document
        EmployeeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        EmployeeDocument doc_ = documentRepository
                .findByIdAndEmployee_UserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Other Document cannot be updated"));


        // ✅ Access control
        validateHierarchyAccess(principal, doc.getEmployee());

        UUID employeeId = doc.getEmployee().getUserId();

        /* ================= DUPLICATE VALIDATIONS ================= */

        // 🔒 1. Document type uniqueness (except current)
        boolean typeExists = documentRepository
                .existsByEmployee_UserIdAndDocumentTypeAndIdNot(
                        employeeId, documentType, documentId);

        if (documentType != DocumentType.OTHER && typeExists) {
            throw new AlreadyExistsResourceException(
                    documentType + " already exists for this employee");
        }

        // 🔒 2. Document number uniqueness (except current)
        if (documentNumber != null && !documentNumber.isBlank()) {

            boolean numberExists = documentRepository
                    .existsByDocumentNumberAndIdNot(documentNumber, documentId);

            if (numberExists) {
                throw new AlreadyExistsResourceException(
                        "Document number already exists");
            }
        }

        /* ================= FILE UPDATE ================= */

        if (file != null && !file.isEmpty()) {

            if (file.getSize() > 5 * 1024 * 1024) {
                throw new BadRequestException("File size exceeds 5MB");
            }

            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.equals("application/pdf") &&
                            !contentType.startsWith("image/"))) {

                throw new BadRequestException("Only PDF and image allowed");
            }

            // 🔒 3. File hash uniqueness
            String fileHash = generateFileHash(file);

            boolean hashExists = documentRepository
                    .existsByFileHashAndIdNot(fileHash, documentId);

            if (hashExists) {
                throw new AlreadyExistsResourceException(
                        "Same document file already exists");
            }

            /* ===== PATH RESOLUTION ===== */

            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path oldPath = projectRoot.resolve(doc.getFilePath()).normalize();

// 🔥 DELETE OLD FILE
            Files.deleteIfExists(oldPath);
            String original = file.getOriginalFilename();
            String cleanName = (original == null) ? "file" :
                    original.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

// create new file
            String newFileName = documentType + "_" + cleanName;
            Path newPath = oldPath.getParent().resolve(newFileName);

            Files.copy(
                    file.getInputStream(),
                    newPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

// update DB
            String newRelativePath = doc.getFilePath()
                    .replace(doc.getFileName(), newFileName);

            doc.setFileName(newFileName);
            doc.setFilePath(newRelativePath);
            doc.setFileHash(fileHash);
        }

        /* ================= META UPDATE ================= */

        doc.setDocumentType(documentType);
        doc.setDocumentNumber(documentNumber);

        documentRepository.save(doc);
    }
}

