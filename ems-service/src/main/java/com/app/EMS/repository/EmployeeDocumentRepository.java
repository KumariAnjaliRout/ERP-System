package com.app.EMS.repository;

import com.app.EMS.entity.DocumentType;
import com.app.EMS.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeDocumentRepository
        extends JpaRepository<EmployeeDocument, Long> {

    List<EmployeeDocument> findByEmployee_EmployeeId(String employeeId);
    List<EmployeeDocument> findByEmployee_UserId(UUID userId);
    Optional<EmployeeDocument> findByEmployeeEmployeeIdAndDocumentType(
            String employeeId,
            DocumentType documentType
    );
    List<EmployeeDocument> findByEmployee_EmployeeIdIn(List<String> employeeIds);
    boolean existsByEmployee_UserIdAndDocumentType(
            UUID userid,
            DocumentType documentType
    );
    boolean existsByDocumentNumber(String documentNumber);
    boolean existsByFileHash(String fileHash);
    Optional<EmployeeDocument> findByIdAndEmployee_UserId(Long documentId,UUID userId);
    Optional<EmployeeDocument> findById(Long id);
    boolean existsByEmployee_UserIdAndDocumentTypeAndIdNot(
            UUID userId, DocumentType type, Long id);

    boolean existsByDocumentNumberAndIdNot(String documentNumber, Long id);

    boolean existsByFileHashAndIdNot(String fileHash, Long id);
}
