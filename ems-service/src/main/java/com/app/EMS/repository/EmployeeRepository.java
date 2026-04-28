package com.app.EMS.repository;

import com.app.EMS.entity.Employee;
import com.app.EMS.entity.EmployeeStatus;
import com.app.EMS.entity.Roles;
import jakarta.persistence.Column;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByUserId(UUID userId);

    boolean existsByEmail(String email);
    long countByStatus(EmployeeStatus status);
    boolean existsByUserId(UUID id);
    boolean existsByEmployeeIdAndStatus(String EmployeeId, EmployeeStatus status);
    boolean existsByIdAndStatus(Long id, EmployeeStatus status);
    Optional<Employee> findById(Long id);
    boolean existsByUserIdAndStatus(UUID userId, EmployeeStatus status);
    Optional<Employee> findByEmployeeIdAndUserId(String employeeId, UUID userId);
    boolean existsByPhone(String phone);
    boolean existsByEmployeeId(String employeeId);
    List<Employee> findByRole(String role);
    List<Employee> findByRole(Roles role);
    List<Employee> findByRoleIn(List<String> roles);
    List<Employee> findByOrganisation(String organisation);


    List<Employee> findByOrganisationAndRole(String organisation, Roles role);

    List<Employee> findByOrganisationAndRoleIn(String organisation, List<String> roles);

//    Page<Employee> findByRole(Roles role, Pageable pageable);
//
//    Page<Employee> findByRoleIn(List<Roles> roles, Pageable pageable);


}