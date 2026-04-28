package com.app.EMS.service;

import com.app.EMS.client.NotificationFeignClient;
import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.LeaveActionRequest;
import com.app.EMS.dto.LeaveApplyRequest;
import com.app.EMS.dto.LeaveResponse;
import com.app.EMS.dto.NotificationRequestDto;
import com.app.EMS.entity.*;
import com.app.EMS.exception.*;
import com.app.EMS.repository.EmployeeLeaveRepository;
import com.app.EMS.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final EmployeeLeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationFeignClient notificationFeignClient;

    /* ========================================
       COMMON VALIDATIONS
    ======================================== */

    private void validateLogin(CustomUserPrincipal principal) {
        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
    }

    private Employee getEmployee(UUID userId) {

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        boolean inactive =
                employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);

        if (inactive)
            throw new BadRequestException("Inactive employee cannot perform action");

        return employee;
    }

    /* ========================================
       ROLE HIERARCHY VALIDATION
    ======================================== */

    private void validateHierarchyAccess(CustomUserPrincipal principal, Employee targetEmployee) {

        String loggedRole = principal.getRole();
        Roles targetRole = targetEmployee.getRole();
        UUID loggedUserId = UUID.fromString(principal.getUserId());

        if (loggedRole.equals("ROLE_SUPER_ADMIN"))
            return;

        if (loggedRole.equals("ROLE_ADMIN")) {

            if (targetEmployee.getUserId().equals(loggedUserId))
                return;
            if (targetRole == Roles.ROLE_HR ||
                    targetRole == Roles.ROLE_MANAGER ||
                    targetRole == Roles.ROLE_ACCOUNTANT ||
                    targetRole == Roles.ROLE_EMPLOYEE)
                return;

            throw new ForbiddenException("Admin cannot access this role");
        }

        if (loggedRole.equals("ROLE_HR")) {

            if (targetEmployee.getUserId().equals(loggedUserId))
                return;

            if (targetRole == Roles.ROLE_EMPLOYEE)
                return;

            throw new ForbiddenException("HR can access only employee data");
        }

        if (!targetEmployee.getUserId().equals(loggedUserId))
            throw new ForbiddenException("You can access only your own data");
    }

    /* ========================================
       APPLY LEAVE
    ======================================== */

    @Transactional
    public void applyLeave(LeaveApplyRequest request, CustomUserPrincipal principal) {

        validateLogin(principal);

        UUID userId = UUID.fromString(principal.getUserId());

        Employee employee = getEmployee(userId);

        if (request.getStartDate().isAfter(request.getEndDate()))
            throw new BadRequestException("Start date cannot be after end date");

        LocalDate today = LocalDate.now();
        if (request.getStartDate().isBefore(today)) {
            throw new BadRequestException("Cannot apply leave for past dates");
        }

        boolean exists =
                leaveRepository.existsByEmployee_UserIdAndStartDateAndEndDateAndLeaveType(
                        userId,
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getLeaveType()
                );

        if (exists)
            throw new AlreadyExistsResourceException("Same leave already applied");

        int days =
                (int) ChronoUnit.DAYS.between(
                        request.getStartDate(),
                        request.getEndDate()
                ) + 1;

        EmployeeLeave leave = EmployeeLeave.builder()
                .employee(employee)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .noOfDays(days)
                .leaveType(request.getLeaveType())
                .reason(request.getReason())
                .requestedByRole(employee.getRole())
                .build();

        leaveRepository.save(leave);
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            NotificationRequestDto notification =
                    NotificationRequestDto.builder()
                            .category(NotificationCategory.LEAVE_MANAGEMENT)
                            .type(NotificationType.LEAVE_REQUEST)
                            .priority(NotificationPriority.NORMAL)
                            .organizationId(principal.getOrganizationId())
                            .metadata(Map.of(
                                    "triggeredByRole", principal.getRole(),
                                    "triggeredByUserId", principal.getUserId(),
                                    "employeeId", employee.getEmployeeId(),
                                    "leaveId", leave.getId()
                            ))
                            .build();
            try {
                notificationFeignClient.sendNotification(notification);
            } catch (Exception ex) {
                log.error("Notification service failed but operation succeeded", ex);
            }
        }
    }

    /* ========================================
       APPROVE / REJECT LEAVE
    ======================================== */

    @Transactional
    public void actionLeave(Long leaveId, LeaveActionRequest request, CustomUserPrincipal principal) {

        validateLogin(principal);
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userId = UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee higher = getEmployee(userId);
        }

        String role=principal.getRole();
        EmployeeLeave leave =
                leaveRepository.findById(leaveId)
                        .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        Employee employee = leave.getEmployee();

//        validateHierarchyAccess(principal, employee);

        Roles applicantRole = leave.getEmployee().getRole();
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform action leave on user in another organization");
            }
        }

        // 🔥 Role-based approval control
        if (role.equals("ROLE_HR") && applicantRole != Roles.ROLE_EMPLOYEE) {
            throw new BadRequestException("HR can only approve Employee leaves");
        }

        if (role.equals("ROLE_ADMIN")
                && !(applicantRole == Roles.ROLE_HR || applicantRole == Roles.ROLE_MANAGER || applicantRole == Roles.ROLE_ACCOUNTANT
                ||applicantRole == Roles.ROLE_EMPLOYEE)) {
            throw new BadRequestException("Admin can only approve HR/Manager/Accountant/Employee leaves");
        }
        if (leave.getStatus() != LeaveStatus.PENDING)
            throw new AlreadyExistsResourceException("Leave already processed");

        leave.setStatus(request.getStatus());
        leave.setRemarks(request.getRemarks());
        leave.setActionedAt(LocalDateTime.now());
        leave.setActionedByRole(
                Roles.valueOf(principal.getRole())
        );
        leaveRepository.save(leave);

        NotificationType type =
                request.getStatus() == LeaveStatus.APPROVED
                        ? NotificationType.LEAVE_APPROVED
                        : NotificationType.LEAVE_REJECTED;

        NotificationRequestDto notification =
                NotificationRequestDto.builder()
                        .category(NotificationCategory.LEAVE_MANAGEMENT)
                        .type(type)
                        .priority(NotificationPriority.HIGH)
                        .organizationId(principal.getOrganizationId())
                        .targetUserId(employee.getUserId())
                        .targetRole(employee.getRole().name().replace("ROLE_", ""))
                        .metadata(Map.of(
                                "triggeredByRole", principal.getRole(),
                                "triggeredByUserId", principal.getUserId(),
                                "leaveId", leave.getId()
                        ))
                        .build();
        try {
            notificationFeignClient.sendNotification(notification);
        } catch (Exception ex) {
            log.error("Notification service failed but operation succeeded", ex);
        }
    }

    /* ========================================
       VIEW MY LEAVES
    ======================================== */

    public List<LeaveResponse> getMyLeaves(CustomUserPrincipal principal) {

        validateLogin(principal);

        UUID userId = UUID.fromString(principal.getUserId());

        getEmployee(userId);

        return leaveRepository.findByEmployee_UserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* ========================================
       VIEW EMPLOYEE LEAVES
    ======================================== */

    public List<LeaveResponse> getLeavesByEmployeeForMonitor(
            String employeeId,
            CustomUserPrincipal principal
    ) {

        validateLogin(principal);
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform action leave on user in another organization");
            }
        }
        validateHierarchyAccess(principal, employee);

        return leaveRepository.findByEmployee_EmployeeId(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* ========================================
       FILTER LEAVES BY STATUS
    ======================================== */

    public List<LeaveResponse> getLeavesByStatus(
            LeaveStatus status,
            CustomUserPrincipal principal
    ) {

        validateLogin(principal);

        String role = principal.getRole();
        String orgId=principal.getOrganizationId();

        List<EmployeeLeave> list;

        switch (role) {

            case "ROLE_SUPER_ADMIN":
                list = leaveRepository.findByStatus(status);
                break;

            case "ROLE_ADMIN":
                list = leaveRepository.findByStatusAndEmployee_OrganisationAndEmployee_RoleIn(
                        status,
                        orgId,
                        List.of(
                                Roles.ROLE_HR,
                                Roles.ROLE_MANAGER,
                                Roles.ROLE_ACCOUNTANT,
                                Roles.ROLE_EMPLOYEE
                        )
                );
                break;

            case "ROLE_HR":
                list = leaveRepository.findByStatusAndEmployee_OrganisationAndEmployee_Role(
                        status,
                        orgId,
                        Roles.ROLE_EMPLOYEE
                );
                break;

            default:
                throw new ForbiddenException("Access denied");
        }

        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* ========================================
       DELETE LEAVE
    ======================================== */

    @Transactional
    public void deleteLeave(Long leaveId, CustomUserPrincipal principal) {

        validateLogin(principal);

        UUID userId = UUID.fromString(principal.getUserId());

        EmployeeLeave leave =
                leaveRepository.findById(leaveId)
                        .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (!leave.getEmployee().getUserId().equals(userId))
            throw new BadRequestException("Cannot delete other employee leave");

        if (leave.getStatus() != LeaveStatus.PENDING)
            throw new BadRequestException("Processed leave cannot be deleted");


        leaveRepository.delete(leave);

        NotificationRequestDto notification =
                NotificationRequestDto.builder()
                        .category(NotificationCategory.LEAVE_MANAGEMENT)
                        .type(NotificationType.LEAVE_CANCELLED)
                        .priority(NotificationPriority.NORMAL)
                        .organizationId(principal.getOrganizationId())
                        .targetUserId(leave.getEmployee().getUserId())
                        .targetRole(
                                leave.getEmployee().getRole()
                                        .name()
                                        .replace("ROLE_", "")
                        )
                        .metadata(Map.of(
                                "triggeredByRole", principal.getRole(),
                                "triggeredByUserId", principal.getUserId(),
                                "leaveId", leave.getId()
                        ))
                        .build();
        try {
            notificationFeignClient.sendNotification(notification);
        } catch (Exception ex) {
            log.error("Notification service failed but operation succeeded", ex);
        }
    }

    /* ========================================
       UPDATE LEAVE
    ======================================== */

    @Transactional
    public void updateLeaveRequest(
            Long leaveId,
            LeaveApplyRequest request,
            CustomUserPrincipal principal
    ) {

        validateLogin(principal);

        EmployeeLeave leave =
                leaveRepository.findById(leaveId)
                        .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (!leave.getEmployee().getUserId().toString()
                .equals(principal.getUserId()))
            throw new BadRequestException("You can update only your leave request");
        if (request.getStartDate().isAfter(request.getEndDate()))
            throw new BadRequestException("Start date cannot be after end date");
        if (leave.getStatus() != LeaveStatus.PENDING)
            throw new BadRequestException("Only pending leave can be updated");

        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setLeaveType(request.getLeaveType());
        leave.setReason(request.getReason());

        leaveRepository.save(leave);

        NotificationRequestDto notification =
                NotificationRequestDto.builder()
                        .category(NotificationCategory.LEAVE_MANAGEMENT)
                        .type(NotificationType.LEAVE_UPDATED)
                        .priority(NotificationPriority.NORMAL)
                        .organizationId(principal.getOrganizationId())
                        .targetUserId(leave.getEmployee().getUserId())
                        .targetRole(
                                leave.getEmployee().getRole()
                                        .name()
                                        .replace("ROLE_", "")
                        )
                        .metadata(Map.of(
                                "triggeredByRole", principal.getRole(),
                                "triggeredByUserId", principal.getUserId(),
                                "leaveId", leave.getId()
                        ))
                        .build();
        try {
            notificationFeignClient.sendNotification(notification);
        } catch (Exception ex) {
            log.error("Notification service failed but operation succeeded", ex);
        }
    }

    /* ========================================
       RESPONSE MAPPER
    ======================================== */
    public List<LeaveResponse> getAllLeaves(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }

        return leaveRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public List<LeaveResponse> monitorLeaves(CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");

        String role = principal.getRole();
        String orgId=principal.getOrganizationId();
        List<EmployeeLeave> list;

        switch (role) {
            case "ROLE_SUPER_ADMIN":
                list = leaveRepository
                        .findAll();
                break;

            case "ROLE_HR":
                list = leaveRepository
                        .findByEmployee_OrganisationAndEmployee_Role(orgId,Roles.ROLE_EMPLOYEE);
                break;

            case "ROLE_ADMIN":
                list = leaveRepository
                        .findByEmployee_OrganisationAndEmployee_RoleIn(orgId,
                                List.of(Roles.ROLE_HR, Roles.ROLE_MANAGER,Roles.ROLE_ACCOUNTANT,Roles.ROLE_EMPLOYEE)
                        );
                break;

            default:
                throw new ForbiddenException("Access denied");
        }

        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private LeaveResponse mapToResponse(EmployeeLeave leave) {

        Employee e = leave.getEmployee();

        return LeaveResponse.builder()
                .leaveId(leave.getId())
                .employeeId(e.getEmployeeId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .noOfDays(leave.getNoOfDays())
                .leaveType(leave.getLeaveType())
                .status(leave.getStatus())
                .reason(leave.getReason())
                .remarks(leave.getRemarks())
                .requestedByRole(leave.getRequestedByRole())
                .actionedByRole(leave.getActionedByRole())
                .build();
    }
}