package com.app.EMS.service;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeCreateRequest request, CustomUserPrincipal principal);

    EmployeeResponse updateEmployee(String employeeId, EmployeeUpdateRequest request, CustomUserPrincipal principal);

    EmployeeResponse getEmployeeById(Long id, CustomUserPrincipal principal);

    List<EmployeeResponse> getAllEmployees( CustomUserPrincipal principal);

    void deactivateEmployee(UUID userId, CustomUserPrincipal principal);
    AuthUserResponse getUserByEmail(String email);
    EmployeeResponse getMyProfile(CustomUserPrincipal principal);
    //Page<EmployeeResponse> getAllEmployees(CustomUserPrincipal principal, int page, int size);
}
