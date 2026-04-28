package com.app.EMS.service;
import com.app.EMS.client.AuthFeignClient;
import com.app.EMS.client.NotificationFeignClient;
import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.*;
import com.app.EMS.entity.*;
import com.app.EMS.exception.*;
import com.app.EMS.repository.EmployeeRepository;
import com.app.EMS.service.EmployeeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final AuthFeignClient authFeignClient;
    private final EmployeeRepository employeeRepository;
    private final NotificationFeignClient notificationFeignClient;

    @Override
    public EmployeeResponse createEmployee(EmployeeCreateRequest request,CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot be create another user");
        }

        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsResourceException("Email already exists");
        }
        AuthUserResponse authUser = authFeignClient.getUserByEmail(request.getEmail());
        String loggedRole = principal.getRole();
        String targetRole = authUser.getRole();// 🔥 From Feign only

        // ORGANIZATION VALIDATION

        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = authUser.getOrganizationId();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot create user in another organization");
            }
        }
        /* ================= ROLE HIERARCHY ================= */

        if (loggedRole.equals("ROLE_ADMIN")) {

            if (!targetRole.equals("HR") &&
                    !targetRole.equals("MANAGER") &&
                    !targetRole.equals("ACCOUNTANT") &&
                    !targetRole.equals("EMPLOYEE")) {

                throw new BadRequestException(
                        "ADMIN can create only HR, MANAGER or ACCOUNTANT,EMPLOYEE"
                );
            }
        }

        else if (loggedRole.equals("ROLE_HR")) {

            if (!targetRole.equals("EMPLOYEE")) {

                throw new BadRequestException(
                        "HR can create only EMPLOYEE"
                );
            }
        }

        // 3️⃣ Validate active
        if (!authUser.isActive()) {
            throw new BadRequestException("User is inactive");
        }
        if (employeeRepository.existsByUserId(authUser.getId())) {
            throw new AlreadyExistsResourceException("User profile already exists");
        }
        if(employeeRepository.existsByEmployeeId(request.getEmployeeId())){
            throw new AlreadyExistsResourceException("EmployeeId already exists");
        }
        if(employeeRepository.existsByPhone(request.getPhone())){
            throw new AlreadyExistsResourceException("Phone number already exists");
        }

        Employee employeee = Employee.builder()
                .userId(authUser.getId())
                .employeeId(request.getEmployeeId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .designation(request.getDesignation())
                .department(request.getDepartment())
                .joinDate(request.getJoinDate())
                .status(EmployeeStatus.ACTIVE)
                .role(request.getRole())
                .organisation(authUser.getOrganizationId())
                .build();
        Employee savedEmployee = employeeRepository.save(employeee);

        /* ================= NOTIFICATION ================= */

        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            notificationFeignClient.sendNotification(
                    NotificationRequestDto.builder()
                            .category(NotificationCategory.EMPLOYEE_MANAGEMENT)
                            .type(NotificationType.EMPLOYEE_CREATED)
                            .priority(NotificationPriority.NORMAL)
                            .organizationId(principal.getOrganizationId())
                            .metadata(Map.of(
                                    "triggeredByRole", principal.getRole(),
                                    "triggeredByUserId", principal.getUserId(),
                                    "employeeId", savedEmployee.getEmployeeId()
                            ))
                            .build()
            );
        }
        return mapToResponse(savedEmployee);
    }

    @Override
    public EmployeeResponse updateEmployee(String employeeId, EmployeeUpdateRequest request, CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        // Only HR or ADMIN allowed
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
                throw new ResourceNotFoundException("Inactive User cannot update another user");
        }
        String loggedRole = principal.getRole();
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByEmployeeIdAndStatus(employeeId, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new ResourceNotFoundException("Inactive User details cannot be updated");
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot update user in another organization");
            }
        }

        Roles targetRole = employee.getRole();

        /* ================= ROLE HIERARCHY ================= */

        if (loggedRole.equals("ROLE_ADMIN")) {

            if (targetRole != Roles.ROLE_HR &&
                    targetRole != Roles.ROLE_MANAGER &&
                    targetRole != Roles.ROLE_ACCOUNTANT
                    && targetRole != Roles.ROLE_EMPLOYEE) {

                throw new BadRequestException(
                        "ADMIN can update only HR, MANAGER or ACCOUNTANT,EMPLOYEE"
                );
            }
        }

        else if (loggedRole.equals("ROLE_HR")) {

            if (targetRole != Roles.ROLE_EMPLOYEE) {

                throw new BadRequestException(
                        "HR can update only EMPLOYEE"
                );
            }
        }

        else {
            throw new ForbiddenException("Access denied");
        }

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        employee.setDesignation(request.getDesignation());
        employee.setDepartment(request.getDepartment());
        employee.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(employeeRepository.save(employee));
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id,CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") &&
                !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot see another user");
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String loggedRole = principal.getRole();
        Roles targetRole = employee.getRole();
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot update user in another organization");
            }
        }

        if (loggedRole.equals("ROLE_SUPER_ADMIN")) {
            return mapToResponse(employee);
        }

        if (loggedRole.equals("ROLE_ADMIN") &&
                (targetRole == Roles.ROLE_HR ||
                        targetRole == Roles.ROLE_MANAGER ||
                        targetRole == Roles.ROLE_ACCOUNTANT ||
                        targetRole==Roles.ROLE_EMPLOYEE)){

            return mapToResponse(employee);
        }

        if (loggedRole.equals("ROLE_HR") &&
                targetRole == Roles.ROLE_EMPLOYEE) {

            return mapToResponse(employee);
        }

        throw new ForbiddenException("Access denied");
    }

    @Override
    public List<EmployeeResponse> getAllEmployees(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") &&
                !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
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
        String role = principal.getRole();
        String orgId=principal.getOrganizationId();
        List<Employee> list;

        switch (role) {
            case "ROLE_SUPER_ADMIN":
                list = employeeRepository.findAll();
                break;

            case "ROLE_HR":

                list = employeeRepository.findByOrganisationAndRole(
                        orgId,
                        Roles.ROLE_EMPLOYEE);
                break;

            case "ROLE_ADMIN":

                list = employeeRepository.findByOrganisationAndRoleIn(
                        orgId,
                        List.of("ROLE_HR", "ROLE_MANAGER", "ROLE_ACCOUNTANT", "ROLE_EMPLOYEE")
                );
                break;

            default:
                throw new ForbiddenException("Access denied");
        }

        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    public void deactivateEmployee(UUID userId,CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        // Only HR or ADMIN allowed
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID token=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(token)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot deactivate other user details");
        }
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new AlreadyExistsResourceException("User already inactive");
        String loggedRole = principal.getRole();
        Roles targetRole = employee.getRole();
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot delete user in another organization");
            }
        }
        if (loggedRole.equals("ROLE_ADMIN")) {

            if (targetRole != Roles.ROLE_HR &&
                    targetRole != Roles.ROLE_MANAGER &&
                    targetRole != Roles.ROLE_ACCOUNTANT &&
                    targetRole != Roles.ROLE_EMPLOYEE
            ) {

                throw new BadRequestException(
                        "ADMIN can deactivate only HR, MANAGER or ACCOUNTANT or EMPLOYEE"
                );
            }
        }

        else if (loggedRole.equals("ROLE_HR")) {

            if (targetRole != Roles.ROLE_EMPLOYEE) {
                throw new BadRequestException(
                        "HR can deactivate only EMPLOYEE"
                );
            }
        }

        employee.setStatus(EmployeeStatus.INACTIVE);
        authFeignClient.toggleUserActivation(userId,false);
        employeeRepository.save(employee);

        /* ================= NOTIFICATION ================= */

        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            notificationFeignClient.sendNotification(
                    NotificationRequestDto.builder()
                            .category(NotificationCategory.EMPLOYEE_MANAGEMENT)
                            .type(NotificationType.EMPLOYEE_DEACTIVATED)
                            .priority(NotificationPriority.CRITICAL)
                            .organizationId(principal.getOrganizationId())
                            .metadata(Map.of(
                                    "triggeredByRole", principal.getRole(),
                                    "triggeredByUserId", principal.getUserId(),
                                    "employeeId", employee.getEmployeeId()
                            ))
                            .build()
            );
        }
    }

    public AuthUserResponse getUserByEmail(String email) {
        return authFeignClient.getUserByEmail(email);
    }
    @Override
    public EmployeeResponse getMyProfile(CustomUserPrincipal principal) {
        if (principal == null)
            throw new UnauthorizedException("Unauthorized access");
        if(!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_MANAGER") &&
                !principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userId = UUID.fromString(principal.getUserId());

        Employee employee = employeeRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User profile not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get your profile.You are inactive user");

        return mapToResponse(employee);
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .userId(employee.getUserId())
                .employeeId(employee.getEmployeeId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .designation(employee.getDesignation())
                .department(employee.getDepartment())
                .status(employee.getStatus())
                .joinDate(employee.getJoinDate())
                .role(Optional.ofNullable(employee.getRole())
                        .map(Enum::name)
                        .orElse(null))
                .organisation(employee.getOrganisation())
                .build();
    }
}
