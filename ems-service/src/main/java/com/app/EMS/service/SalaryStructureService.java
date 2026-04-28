package com.app.EMS.service;

import com.app.EMS.client.NotificationFeignClient;
import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.NotificationRequestDto;
import com.app.EMS.dto.SalaryStructureRequest;
import com.app.EMS.dto.SalaryStructureResponse;
import com.app.EMS.entity.*;
import com.app.EMS.exception.*;
import com.app.EMS.repository.EmployeeRepository;
import com.app.EMS.repository.SalaryStructureRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
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
public class SalaryStructureService {

    private final SalaryStructureRepository repository;
    private final EmployeeRepository employeeRepository;
    private final NotificationFeignClient notificationFeignClient;

    /* =====================================
       COMMON VALIDATIONS
    ===================================== */

    private void validateLogin(CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
    }

    private Employee getEmployeeByEmployeeId(String employeeId){

        return employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private Employee getEmployeeByUserId(UUID userId){

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        boolean inactive =
                employeeRepository.existsByEmployeeIdAndStatus(
                        employee.getEmployeeId(),
                        EmployeeStatus.INACTIVE
                );

        if(inactive)
            throw new BadRequestException("Inactive employee cannot perform this action");

        return employee;
    }

    /* =====================================
       ROLE HIERARCHY VALIDATION
    ===================================== */

    private void validateHierarchyAccess(CustomUserPrincipal principal, Employee targetEmployee){

        String loggedRole = principal.getRole();
        Roles targetRole = targetEmployee.getRole();
        UUID loggedUserId = UUID.fromString(principal.getUserId());

        if(loggedRole.equals("ROLE_SUPER_ADMIN"))
            return;

        if(loggedRole.equals("ROLE_ADMIN")){

            if(targetEmployee.getUserId().equals(loggedUserId))
                return;

            if(targetRole == Roles.ROLE_HR
                    || targetRole == Roles.ROLE_MANAGER
                    || targetRole == Roles.ROLE_ACCOUNTANT
                    || targetRole==Roles.ROLE_EMPLOYEE)
                return;

            throw new ForbiddenException("Admin cannot access this role");
        }

        if(loggedRole.equals("ROLE_HR")){

            if(targetEmployee.getUserId().equals(loggedUserId))
                return;

            if(targetRole == Roles.ROLE_EMPLOYEE)
                return;

            throw new ForbiddenException("HR can access only employee data");
        }

        if(!targetEmployee.getUserId().equals(loggedUserId))
            throw new ForbiddenException("You can access only your own data");
    }

    /* =====================================
       CREATE SALARY STRUCTURE
    ===================================== */

    @Transactional
    public SalaryStructureResponse create(SalaryStructureRequest request, CustomUserPrincipal principal){

        validateLogin(principal);
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            getEmployeeByUserId(userid);
        }
        Employee employee = getEmployeeByEmployeeId(request.getEmployeeId());

        validateHierarchyAccess(principal, employee);

        if(repository.existsByEmployeeId(request.getEmployeeId()))
            throw new AlreadyExistsResourceException("Salary structure already exists");
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform this action on users in another organization");
            }
        }

        SalaryStructure salary = mapToEntity(request);

        salary.setCreatedByRole(principal.getRole());

        if(principal.getRole().equals("ROLE_ADMIN")
                || principal.getRole().equals("ROLE_SUPER_ADMIN")){

            salary.setStatus(ApprovalStatus.APPROVED);
            salary.setApprovedDate(LocalDate.now());

        }else{

            salary.setStatus(ApprovalStatus.PENDING);
        }

        SalaryStructure saved = repository.save(salary);

        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            NotificationRequestDto notification =
                    NotificationRequestDto.builder()
                            .category(NotificationCategory.PAYROLL)
                            .type(NotificationType.SALARY_STRUCTURE_CREATED)
                            .priority(NotificationPriority.NORMAL)
                            .organizationId(principal.getOrganizationId())
                            .targetUserId(employee.getUserId())
                            .targetRole(employee.getRole().name())
                            .metadata(Map.of(
                                    "triggeredByRole", principal.getRole(),
                                    "triggeredByUserId", principal.getUserId(),
                                    "employeeId", saved.getEmployeeId()
                            ))
                            .build();

            try {
                notificationFeignClient.sendNotification(notification);
            } catch (FeignException ex) {
                log.error("Notification failed but operation succeeded", ex);
            }
        }

        return mapToResponse(saved);
    }

    /* =====================================
       VIEW SALARY OF EMPLOYEE
    ===================================== */

    public SalaryStructureResponse monitorSalary(String employeeId, CustomUserPrincipal principal){

        validateLogin(principal);
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            getEmployeeByUserId(userid);
        }
        Employee employee = getEmployeeByEmployeeId(employeeId);

        validateHierarchyAccess(principal, employee);
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform this action on users in another organization");
            }
        }
        SalaryStructure salary = repository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));

        return mapToResponse(salary);
    }

    /* =====================================
       VIEW OWN SALARY
    ===================================== */

    public SalaryStructureResponse getMySalary(CustomUserPrincipal principal){

        validateLogin(principal);

        UUID userId = UUID.fromString(principal.getUserId());

        Employee self = getEmployeeByUserId(userId);

        SalaryStructure salary = repository.findByEmployeeId(self.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));

        if(salary.getStatus()!=ApprovalStatus.APPROVED)
            throw new ResourceNotFoundException("Salary not approved yet");

        return mapToResponse(salary);
    }

    /* =====================================
       UPDATE SALARY
    ===================================== */

    @Transactional
    public SalaryStructureResponse update(String employeeId, SalaryStructureRequest req, CustomUserPrincipal principal){

        validateLogin(principal);
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            getEmployeeByUserId(userid);
        }

        Employee employee = getEmployeeByEmployeeId(employeeId);
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform this action on users in another organization");
            }
        }
        validateHierarchyAccess(principal, employee);

        SalaryStructure s = repository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));

        s.setBasic(req.getBasic());
        s.setHra(req.getHra());
        s.setTravelAllowance(req.getTravelAllowance());
        s.setMedicalAllowance(req.getMedicalAllowance());
        s.setShiftAllowance(req.getShiftAllowance());
        s.setOtherAllowance(req.getOtherAllowance());
        s.setPf(req.getPf());
        s.setProfessionalTax(200.0);
        s.setVariablePay(req.getVariablePay());
        s.setAnnualCtc(req.getAnnualCtc());
        s.setStatus(ApprovalStatus.PENDING);
        s.setEffectiveFrom(req.getEffectiveFrom());

        s.setGrossFixedPay(
                req.getBasic()
                        + req.getHra()
                        + req.getTravelAllowance()
                        + req.getMedicalAllowance()
                        + req.getShiftAllowance()
                        + req.getOtherAllowance()
        );

        SalaryStructure updated = repository.save(s);

        NotificationRequestDto notification =
                NotificationRequestDto.builder()
                        .category(NotificationCategory.PAYROLL)
                        .type(NotificationType.SALARY_STRUCTURE_UPDATED)
                        .priority(NotificationPriority.NORMAL)
                        .organizationId(principal.getOrganizationId())
                        .metadata(Map.of(
                                "triggeredByRole", principal.getRole(),
                                "triggeredByUserId", principal.getUserId(),
                                "employeeId", updated.getEmployeeId()
                        ))
                        .build();

        try {
            notificationFeignClient.sendNotification(notification);
        } catch (Exception ex) {
            log.error("Notification failed but operation succeeded", ex);
        }


        return mapToResponse(updated);
    }

    /* =====================================
       DELETE SALARY STRUCTURE
    ===================================== */

    @Transactional
    public void delete(String employeeId,CustomUserPrincipal principal){
        validateLogin(principal);
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            getEmployeeByUserId(userid);
        }
        Employee employee = getEmployeeByEmployeeId(employeeId);
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform this action on users in another organization");
            }
        }
        validateHierarchyAccess(principal, employee);

        SalaryStructure salary = repository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));

        repository.delete(salary);
        //notification
        NotificationRequestDto notification =
                NotificationRequestDto.builder()
                        .category(NotificationCategory.PAYROLL)
                        .type(NotificationType.SALARY_STRUCTURE_DELETED)
                        .priority(NotificationPriority.CRITICAL)
                        .organizationId(principal.getOrganizationId())
                        .metadata(Map.of(
                                "triggeredByRole", principal.getRole(),
                                "triggeredByUserId", principal.getUserId(),
                                "employeeId", salary.getEmployeeId()
                        ))
                        .build();

        try {
            notificationFeignClient.sendNotification(notification);
        } catch (FeignException ex) {
            log.error("Notification failed but operation succeeded", ex);
        }
    }

    /* =====================================
       GET ALL SALARIES BASED ON ROLE
    ===================================== */

    public List<SalaryStructureResponse> getAll(CustomUserPrincipal principal){

        validateLogin(principal);
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            getEmployeeByUserId(userid);
        }

        String role = principal.getRole();
        String orgId = principal.getOrganizationId();

        List<SalaryStructure> list;

        switch(role){

            case "ROLE_SUPER_ADMIN":

                list = repository.findAll();
                break;

            case "ROLE_ADMIN":

                List<Employee> admins = employeeRepository
                        .findByOrganisationAndRoleIn(orgId,
                                List.of(
                                        "ROLE_HR",
                                        "ROLE_MANAGER",
                                        "ROLE_ACCOUNTANT",
                                        "ROLE_EMPLOYEE"
                                )
                        );

                List<String> ids1 =
                        admins.stream().map(Employee::getEmployeeId).toList();

                list = repository.findByEmployeeIdIn(ids1);
                break;

            case "ROLE_HR":

                List<Employee> employees = employeeRepository
                        .findByOrganisationAndRole(orgId,Roles.ROLE_EMPLOYEE);

                List<String> ids2 =
                        employees.stream().map(Employee::getEmployeeId).toList();

                list = repository.findByEmployeeIdIn(ids2);
                break;

            case "ROLE_ACCOUNTANT":

//                list = repository.findByStatusAndOrganisation(ApprovalStatus.APPROVED,orgId);
//                break;

                List<Employee> employee = employeeRepository
                        .findByOrganisationAndRoleIn(
                                orgId,
                                List.of(
                                        "ROLE_HR",
                                        "ROLE_MANAGER",
                                        "ROLE_ACCOUNTANT",
                                        "ROLE_EMPLOYEE"
                                )
                        );

                List<String> employeeIds = employee.stream()
                        .map(Employee::getEmployeeId)
                        .toList();

                list = repository.findByEmployeeIdInAndStatus(
                        employeeIds,
                        ApprovalStatus.APPROVED
                );

                break;
            default:

                throw new ForbiddenException("Access denied");
        }

        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }
    public List<SalaryStructureResponse> getAllbyOfficials(CustomUserPrincipal principal){

        validateLogin(principal);

        String role = principal.getRole();

        if(!role.equals("ROLE_SUPER_ADMIN")){
            throw new ForbiddenException("Access denied");
        }

        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public List<SalaryStructureResponse> getAllApproved(CustomUserPrincipal principal){

        validateLogin(principal);

        String role = principal.getRole();
        String orgId = principal.getOrganizationId();
        List<SalaryStructure> list;

        switch (role){

            case "ROLE_SUPER_ADMIN":
                list = repository.findByStatus(ApprovalStatus.APPROVED);
                break;

            case "ROLE_ADMIN":

                List<Employee> admins =
                        employeeRepository.findByOrganisationAndRoleIn(orgId,
                                List.of(
                                        "ROLE_HR",
                                        "ROLE_MANAGER",
                                        "ROLE_ACCOUNTANT",
                                        "ROLE_EMPLOYEE"
                                )
                        );

                List<String> ids1 = admins.stream()
                        .map(Employee::getEmployeeId)
                        .toList();

                list = repository.findByEmployeeIdInAndStatus(
                        ids1,
                        ApprovalStatus.APPROVED
                );

                break;

            case "ROLE_HR":

                List<Employee> employees =
                        employeeRepository.findByOrganisationAndRole(orgId,Roles.ROLE_EMPLOYEE);

                List<String> ids2 = employees.stream()
                        .map(Employee::getEmployeeId)
                        .toList();

                list = repository.findByEmployeeIdInAndStatus(
                        ids2,
                        ApprovalStatus.APPROVED
                );

                break;

            case "ROLE_ACCOUNTANT":

                List<Employee> employee= employeeRepository
                        .findByOrganisationAndRoleIn(
                                orgId,
                                List.of(
                                        "ROLE_HR",
                                        "ROLE_MANAGER",
                                        "ROLE_ACCOUNTANT",
                                        "ROLE_EMPLOYEE"
                                )
                        );

                List<String> employeeIds = employee.stream()
                        .map(Employee::getEmployeeId)
                        .toList();

                list = repository.findByEmployeeIdInAndStatus(
                        employeeIds,
                        ApprovalStatus.APPROVED
                );
                break;

            default:
                throw new ForbiddenException("Access denied");
        }

        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* =====================================
       MAPPERS
    ===================================== */

    private Double nullSafe(Double val){

        return val == null ? 0.0 : val;
    }

    private SalaryStructure mapToEntity(SalaryStructureRequest r){

        return SalaryStructure.builder()
                .employeeId(r.getEmployeeId())
                .basic(nullSafe(r.getBasic()))
                .hra(nullSafe(r.getHra()))
                .travelAllowance(nullSafe(r.getTravelAllowance()))
                .medicalAllowance(nullSafe(r.getMedicalAllowance()))
                .shiftAllowance(nullSafe(r.getShiftAllowance()))
                .otherAllowance(nullSafe(r.getOtherAllowance()))
                .pf(nullSafe(r.getPf()))
                .professionalTax(200.0)
                .variablePay(nullSafe(r.getVariablePay()))
                .annualCtc(nullSafe(r.getAnnualCtc()))
                .effectiveFrom(r.getEffectiveFrom())
                .status(ApprovalStatus.PENDING)
                .build();
    }

    private SalaryStructureResponse mapToResponse(SalaryStructure s){

        return SalaryStructureResponse.builder()
                .id(s.getId())
                .employeeId(s.getEmployeeId())
                .basic(s.getBasic())
                .hra(s.getHra())
                .travelAllowance(s.getTravelAllowance())
                .medicalAllowance(s.getMedicalAllowance())
                .shiftAllowance(s.getShiftAllowance())
                .otherAllowance(s.getOtherAllowance())
                .grossFixedPay(s.getGrossFixedPay())
                .pf(s.getPf())
                .professionalTax(s.getProfessionalTax())
                .variablePay(s.getVariablePay())
                .annualCtc(s.getAnnualCtc())
                .status(s.getStatus())
                .effectiveFrom(s.getEffectiveFrom())
                .build();
    }
}