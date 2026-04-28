package com.app.EMS.repository;

import com.app.EMS.entity.EmployeePersonalDetails;
import jakarta.persistence.Column;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeePersonalDetailsRepository
        extends JpaRepository<EmployeePersonalDetails, Long> {

    Optional<EmployeePersonalDetails> findByEmployeeEmployeeId(String employeeId);
    Optional<EmployeePersonalDetails>  findByEmployeeUserId(UUID userId);
    //    boolean existsByAccountHolderName(String accountHolderName);
//    boolean existsByAccountNumber(String accountNumber);
//    boolean existsByIfscCode(String ifscCode);
//    boolean existsByPfAccountNumber(String pfAccountNumber);
    boolean existsByAccountHolderNameAndEmployeeUserIdNot(String accountHolderName, UUID userId);

    boolean existsByAccountNumberAndEmployeeUserIdNot(String accountNumber, UUID userId);

    boolean existsByIfscCodeAndEmployeeUserIdNot(String ifscCode, UUID userId);

    boolean existsByPfAccountNumberAndEmployeeUserIdNot(String pfAccountNumber, UUID userId);
}
