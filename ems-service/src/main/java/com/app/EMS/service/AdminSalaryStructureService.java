package com.app.EMS.service;

import com.app.EMS.client.NotificationFeignClient;
import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.NotificationRequestDto;
import com.app.EMS.entity.*;
import com.app.EMS.exception.BadRequestException;
import com.app.EMS.exception.ForbiddenException;
import com.app.EMS.exception.ResourceNotFoundException;
import com.app.EMS.exception.UnauthorizedException;
import com.app.EMS.repository.EmployeeRepository;
import com.app.EMS.repository.SalaryStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSalaryStructureService {

    private final SalaryStructureRepository repo;
    private final EmployeeRepository employeeRepository;
    private final NotificationFeignClient notificationFeignClient;

    /* ADMIN APPROVES */
    public SalaryStructure approve(String id, CustomUserPrincipal principal){
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if ((!principal.getRole().equals("ROLE_ADMIN") && (!principal.getRole().equals("ROLE_SUPER_ADMIN") && !principal.getRole().equals("ROLE_HR"))))  {
            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot perform this action");
        }
        SalaryStructure s = repo.findByEmployeeId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found"));
        Employee employee=employeeRepository.findByEmployeeId(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform this action on users in another organization");
            }
        }

        s.setStatus(ApprovalStatus.APPROVED);
        s.setApprovedDate(LocalDate.now());
        s.setAdminRemarks(null);

        SalaryStructure saved = repo.save(s);
//        Employee employee = employeeRepository
//                .findByEmployeeId(s.getEmployeeId())
//                .orElseThrow(() -> new RuntimeException("Employee not found"));

        try {

            NotificationRequestDto notification = NotificationRequestDto.builder()
                    .category(NotificationCategory.PAYROLL)
                    .type(NotificationType.SALARY_STRUCTURE_APPROVED)
                    .priority(NotificationPriority.HIGH)
                    .organizationId(principal.getOrganizationId())
                    .targetRole("HR")
                    .metadata(Map.of(
                            "triggeredByRole", principal.getRole(),
                            "triggeredByUserId", principal.getUserId(),
                            "employeeId", saved.getEmployeeId()
                    ))
                    .build();

            notificationFeignClient.sendNotification(notification);

        } catch (Exception ex) {
            log.error("Notification failed for SALARY_STRUCTURE_APPROVED", ex);
        }
        return saved;
    }

    /* ADMIN REJECTS */
    public SalaryStructure reject(String id, String remarks,CustomUserPrincipal principal){
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if ((!principal.getRole().equals("ROLE_ADMIN") && (!principal.getRole().equals("ROLE_SUPER_ADMIN") && !principal.getRole().equals("ROLE_HR"))))  {
            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot perform this action");
        }
        SalaryStructure s = repo.findByEmployeeId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found"));
        Employee employee=employeeRepository.findByEmployeeId(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform this action on users in another organization");
            }
        }

        s.setStatus(ApprovalStatus.REJECTED);
        s.setApprovedDate(LocalDate.now());
        s.setAdminRemarks(remarks);
        SalaryStructure saved = repo.save(s);
//        Employee employee = employeeRepository
//                .findByEmployeeId(s.getEmployeeId())
//                .orElseThrow(() -> new RuntimeException("Employee not found"));

        //notification
        try {

            NotificationRequestDto notification = NotificationRequestDto.builder()
                    .category(NotificationCategory.PAYROLL)
                    .type(NotificationType.SALARY_STRUCTURE_REJECTED)
                    .priority(NotificationPriority.HIGH)
                    .organizationId(principal.getOrganizationId())
                    .targetRole("HR")
                    .metadata(Map.of(
                            "triggeredByRole", principal.getRole(),
                            "triggeredByUserId", principal.getUserId(),
                            "employeeId", saved.getEmployeeId(),
                            "remarks", remarks == null ? "" : remarks
                    ))
                    .build();

            notificationFeignClient.sendNotification(notification);

        } catch (Exception ex) {
            log.error("Notification failed for SALARY_STRUCTURE_REJECTED", ex);
        }
        return saved;
    }



    public List<SalaryStructure> pending(CustomUserPrincipal principal){

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if ((!principal.getRole().equals("ROLE_ADMIN") && (!principal.getRole().equals("ROLE_SUPER_ADMIN") && !principal.getRole().equals("ROLE_HR"))))  {
            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot perform this action");
        }
        String role = principal.getRole();
        String orgId = principal.getOrganizationId();

        if (role.equals("ROLE_SUPER_ADMIN"))
            return repo.findByStatus(ApprovalStatus.PENDING);

        if (role.equals("ROLE_HR")) {

            List<String> ids = employeeRepository
                    .findByOrganisationAndRole(orgId,Roles.ROLE_EMPLOYEE)
                    .stream()
                    .map(Employee::getEmployeeId)
                    .toList();

            return repo.findByEmployeeIdInAndStatus(ids, ApprovalStatus.PENDING);
        }

        if (role.equals("ROLE_ADMIN")) {

            List<String> ids = employeeRepository
                    .findByOrganisationAndRoleIn(orgId,List.of("ROLE_HR","ROLE_MANAGER","ROLE_EMPLOYEE","ROLE_ACCOUNTANT"))
                    .stream()
                    .map(Employee::getEmployeeId)
                    .toList();

            return repo.findByEmployeeIdInAndStatus(ids, ApprovalStatus.PENDING);
        }
        throw new ForbiddenException("Access denied");
    }

    public List<SalaryStructure> approved(CustomUserPrincipal principal){

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if ((!principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && (!principal.getRole().equals("ROLE_SUPERADMIN") && !principal.getRole().equals("HR"))))  {
            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot perform this action");
        }
        String role = principal.getRole();
        String orgId = principal.getOrganizationId();


        if (role.equals("ROLE_SUPERADMIN") || role.equals("ROLE_ACCOUNTANT"))
            return repo.findByStatus(ApprovalStatus.APPROVED);

        if (role.equals("ROLE_HR")) {

            List<String> ids = employeeRepository
                    .findByOrganisationAndRole(orgId,Roles.ROLE_EMPLOYEE)
                    .stream()
                    .map(Employee::getEmployeeId)
                    .toList();

            return repo.findByEmployeeIdInAndStatus(ids, ApprovalStatus.APPROVED);
        }

        if (role.equals("ROLE_ADMIN")) {

            List<String> ids = employeeRepository
                    .findByOrganisationAndRoleIn(orgId,List.of("ROLE_HR","ROLE_MANAGER","ROLE_ACCOUNTANT","ROLE_EMPLOYEE"))
                    .stream()
                    .map(Employee::getEmployeeId)
                    .toList();

            return repo.findByEmployeeIdInAndStatus(ids, ApprovalStatus.APPROVED);
        }

        throw new ForbiddenException("Access denied");
    }
    public List<SalaryStructure> rejected(CustomUserPrincipal principal){

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if ((!principal.getRole().equals("ROLE_ADMIN") && (!principal.getRole().equals("ROLE_SUPER_ADMIN") && !principal.getRole().equals("HR"))))  {
            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot perform this action");
        }
        String role = principal.getRole();
        String orgId= principal.getOrganizationId();

        if (role.equals("ROLE_SUPERADMIN"))
            return repo.findByStatus(ApprovalStatus.REJECTED);

        if (role.equals("ROLE_HR")) {

            List<String> ids = employeeRepository
                    .findByOrganisationAndRole(orgId,Roles.ROLE_EMPLOYEE)
                    .stream()
                    .map(Employee::getEmployeeId)
                    .toList();

            return repo.findByEmployeeIdInAndStatus(ids, ApprovalStatus.REJECTED);
        }

        if (role.equals("ROLE_ADMIN")) {

            List<String> ids = employeeRepository
                    .findByOrganisationAndRoleIn(orgId,List.of("ROLE_HR","ROLE_MANAGER","ROLE_ACCOUNTANT","ROLE_EMPLOYEE"))
                    .stream()
                    .map(Employee::getEmployeeId)
                    .toList();

            return repo.findByEmployeeIdInAndStatus(ids, ApprovalStatus.REJECTED);
        }

        throw new ForbiddenException("Access denied");
    }
}
