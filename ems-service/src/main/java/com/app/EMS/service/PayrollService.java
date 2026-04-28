package com.app.EMS.service;

import com.app.EMS.client.NotificationFeignClient;
import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.NotificationRequestDto;
import com.app.EMS.dto.PayrollRequest;
import com.app.EMS.dto.PayrollResponse;
import com.app.EMS.dto.PayrollSummaryDto;
import com.app.EMS.entity.*;
import com.app.EMS.exception.*;
import com.app.EMS.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollService {

    private final PayrollRepository payrollRepo;
    private final SalaryStructureRepository salaryRepo;
    private final AttendanceRepository attendanceRepo;
    private final EmployeeRepository employeeRepository;
    private final EmployeeLeaveRepository leaveRepository;
    private final NotificationFeignClient notificationFeignClient;

    private void validateHierarchyAccess(CustomUserPrincipal principal, Employee target) {

        String loggedRole = principal.getRole();
        Roles targetRole = target.getRole();
        UUID loggedUserId = UUID.fromString(principal.getUserId());

        // SUPER ADMIN → full access
        if (loggedRole.equals("ROLE_SUPER_ADMIN")) {
            return;
        }

        // ADMIN → HR, MANAGER, ACCOUNTANT + own
        if (loggedRole.equals("ROLE_ADMIN")) {

//            if(target.getUserId().equals(loggedUserId))
//                return;

            if (targetRole == Roles.ROLE_HR ||
                    targetRole == Roles.ROLE_MANAGER ||
                    targetRole == Roles.ROLE_ACCOUNTANT ||
                    targetRole == Roles.ROLE_EMPLOYEE)
                return;

            throw new ForbiddenException("Admin cannot access this payroll");
        }

        // HR → EMPLOYEE + own
        if (loggedRole.equals("ROLE_HR")) {

//            if(target.getUserId().equals(loggedUserId))
//                return;

            if (targetRole == Roles.ROLE_EMPLOYEE)
                return;

            throw new ForbiddenException("HR can access only employee payroll");
        }

    }

    public double calculate(double monthlySalary) {

        if (monthlySalary <= 15000)
            return 0.0;

        if (monthlySalary <= 20000)
            return 150;

        return 200;
    }

    public PayrollResponse generatePayroll(PayrollRequest request, CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        String role = principal.getRole();
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
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
        Employee employee=employeeRepository.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform this action on users in another organization");
            }
        }
        if (request.getStartDate().isAfter(request.getEndDate()))
            throw new BadRequestException("Start date cannot be after end date");
        long totalDays =
                ChronoUnit.DAYS.between(
                        request.getStartDate(),
                        request.getEndDate()
                ) + 1;

        if (totalDays < 28) {
            throw new BadRequestException(
                    "Payroll range must be minimum 28 days. Please enter valid payroll dates"
            );
        }
        int month = request.getStartDate().getMonthValue();
        int year = request.getStartDate().getYear();

        if (payrollRepo.existsByEmployee_EmployeeIdAndMonthAndYear(
                request.getEmployeeId(), month, year))
            throw new AlreadyExistsResourceException("Payroll already generated for this month");


        /* ========= EMPLOYEE ========= */


        Roles targetRole = employee.getRole();
        validateHierarchyAccess(principal, employee);

        SalaryStructure salary =
                salaryRepo.findByEmployeeId(request.getEmployeeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));
        if (salary.getStatus() != ApprovalStatus.APPROVED) {
            throw new BadRequestException(
                    "Salary structure not approved"
            );
        }
//
//        SalaryStructure status=salaryRepo.findByStatus(ApprovalStatus.APPROVED);
        /* ========= ATTENDANCE ========= */

        List<Attendance> records =
                attendanceRepo.findByEmployee_EmployeeIdAndDateBetween(
                        request.getEmployeeId(),
                        request.getStartDate(),
                        request.getEndDate()
                );

        int present = 0;
        int half = 0;

        for (Attendance a : records) {

            if (a.getStatus() == null) continue;

            switch (a.getStatus()) {
                case PRESENT -> present++;
                case HALF_DAY -> half++;
            }
        }


        /* ========= TOTAL DAYS ========= */


        /* ========= WEEKENDS ========= */

        int weekends = 0;

        for (int i = 0; i < totalDays; i++) {
            var d = request.getStartDate().plusDays(i).getDayOfWeek();

            if (d == java.time.DayOfWeek.SATURDAY ||
                    d == java.time.DayOfWeek.SUNDAY)
                weekends++;
        }


        /* ========= HOLIDAYS ========= */
        int holidays = 0; // integrate Holiday table later


        /* ========= WORKING DAYS ========= */

        int workingDays = (int) totalDays - weekends - holidays;

        if (workingDays <= 0)
            throw new ResourceNotFoundException("Invalid working days");


        /* ========= ABSENT ========= */

//        int absent = workingDays - present - half;
//        if (absent < 0) absent = 0;

        List<EmployeeLeave> paidLeaves =
                leaveRepository
                        .findByEmployee_EmployeeIdAndStatusAndLeaveTypeAndStartDateBetween(
                                request.getEmployeeId(),
                                LeaveStatus.APPROVED,
                                LeaveType.PAID,
                                request.getStartDate(),
                                request.getEndDate()
                        );
        int paidLeaveDays = paidLeaves.stream()
                .mapToInt(EmployeeLeave::getNoOfDays)
                .sum();
        int absent = workingDays - present - half - paidLeaveDays;
        if (absent < 0) absent = 0;


        /* ========= PAYABLE ========= */
//
//        double payableDays = present + (half * 0.5);
        double payableDays = present + (half * 0.5) + paidLeaveDays;


        /* ========= SALARY CALCULATION ========= */

        double monthlySalary = salary.getGrossFixedPay();

        double dailySalary = monthlySalary / workingDays;

        double grossSalary = dailySalary * payableDays;

        double ratio = payableDays / workingDays;

//        double pf=salary.getBasic();
        double pfPercent = 12;

        double earnedBasic = salary.getBasic() * (payableDays / workingDays);

        double pf = earnedBasic * pfPercent / 100;
//        double pf  = (salary.getPf() == null ? 0 : salary.getPf()) * ratio;
        double tax = (grossSalary == 0 ? 0.0 : calculate(monthlySalary));

        double deductions = pf + tax;

        double netSalary = grossSalary - deductions;

        if (netSalary < 0)
            netSalary = 0;


        /* ========= ROUND VALUES ========= */

        grossSalary = Math.round(grossSalary * 100.0) / 100.0;
        netSalary = Math.round(netSalary * 100.0) / 100.0;
        pf = Math.round(pf * 100.0) / 100.0;
        tax = Math.round(tax * 100.0) / 100.0;



        /* ========= SAVE ========= */

        Payroll payroll = Payroll.builder()
                .employee(employee)
                .month(month)
                .year(year)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(workingDays)
                .presentDays(present + paidLeaveDays)
                .halfDays(half)
                .absentDays(absent)
                .grossSalary(grossSalary)
                .totalDeductions(pf + tax)
                .netSalary(netSalary)
                .status(PayrollStatus.GENERATED)
                .pf(pf)
                .professionalTax(tax)
                .generatedAt(LocalDateTime.now())
                .generatedByRole(Roles.valueOf(role))
                .build();

        Payroll savedPayroll = payrollRepo.save(payroll);

        //notification
        try {

            NotificationRequestDto notification = NotificationRequestDto.builder()
                    .category(NotificationCategory.PAYROLL)
                    .type(NotificationType.PAYROLL_GENERATED)
                    .priority(NotificationPriority.HIGH)
                    .organizationId(principal.getOrganizationId())
                    .targetUserId(employee.getUserId())
                    .targetRole(employee.getRole().name().replace("ROLE_", ""))
                    .metadata(Map.of(
                            "triggeredByRole", principal.getRole(),
                            "triggeredByUserId", principal.getUserId(),
                            "employeeId", employee.getEmployeeId(),
                            "month", month,
                            "year", year,
                            "netSalary", netSalary
                    ))
                    .build();

            notificationFeignClient.sendNotification(notification);

        } catch (Exception ex) {
            log.error("Notification failed for PAYROLL_GENERATED", ex);
        }

        /* ========= RESPONSE ========= */

        return PayrollResponse.builder()
                .id(savedPayroll.getId())
                .employeeId(request.getEmployeeId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(workingDays)
                .presentDays(present + paidLeaveDays)
                .halfDays(half)
                .absentDays(absent)
                .payableDays(payableDays)
                .grossSalary(grossSalary)
                .pf(pf)
                .professionalTax(tax)
                .netSalary(netSalary)
                .status(PayrollStatus.GENERATED)
                .pf(pf)
                .professionalTax(tax)
                .generatedAt(LocalDateTime.now())
                .generatedByRole(Roles.valueOf(role))
                .build();
    }


    @Transactional
    public void markPaid(Long payrollId, CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
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

        Payroll p = payrollRepo.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found"));

        Employee target = p.getEmployee();
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = target.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform this action on users in another organization");
            }
        }
        System.out.println("TARGET ROLE = " + target.getRole());

        if (target == null)
            throw new ResourceNotFoundException("Employee not attached to payroll");

        validateHierarchyAccess(principal, target);
        /* ================= UPDATE STATUS ================= */
        p.setStatus(PayrollStatus.PAID);
        payrollRepo.save(p);
        try {
            NotificationRequestDto employeeNotification = NotificationRequestDto.builder()
                    .category(NotificationCategory.PAYROLL)
                    .type(NotificationType.SALARY_PAID)
                    .priority(NotificationPriority.CRITICAL)
                    .organizationId(principal.getOrganizationId())
                    .targetUserId(target.getUserId())
                    .targetRole(target.getRole().name().replace("ROLE_", ""))
                    .metadata(Map.of(
                            "triggeredByRole", principal.getRole(),
                            "employeeId", target.getEmployeeId(),
                            "month", p.getMonth(),
                            "year", p.getYear(),
                            "netSalary", p.getNetSalary()
                    ))
                    .build();

            notificationFeignClient.sendNotification(employeeNotification);
        }
        catch (Exception ex) {
            log.error("Notification failed for PAYROLL_GENERATED", ex);
        }
    }


    public List<PayrollResponse> getMonthly(int month,
                                            int year,
                                            CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");

        String role = principal.getRole();

        if (!role.equals("ROLE_HR") &&
                !role.equals("ROLE_ADMIN") &&
                !role.equals("ROLE_SUPER_ADMIN"))
            throw new ForbiddenException("Access denied");
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot perform this action");
        }
        String orgId=principal.getOrganizationId();
        List<Payroll> payrolls = payrollRepo.findByEmployee_OrganisationAndMonthAndYear(orgId,month, year);

        if (role.equals("ROLE_HR")) {

            return payrolls.stream()
                    .filter(p -> p.getEmployee().getRole() == Roles.ROLE_EMPLOYEE)
                    .map(this::map)
                    .toList();
        }

        if (role.equals("ROLE_ADMIN")) {

            return payrolls.stream()
                    .filter(p ->
                            p.getEmployee().getRole() == Roles.ROLE_HR ||
                                    p.getEmployee().getRole() == Roles.ROLE_MANAGER ||
                                    p.getEmployee().getRole() == Roles.ROLE_ACCOUNTANT||
                                    p.getEmployee().getRole() == Roles.ROLE_EMPLOYEE
                    )
                    .map(this::map)
                    .toList();
        }
        return payrolls.stream()
                .map(this::map)
                .toList();
    }


    public List<PayrollResponse> monitorPayroll(String employeeId, CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");

        String role = principal.getRole();
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot perform this action");
        }
        Employee target = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = target.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot perform this action on users in another organization");
            }
        }

        Roles targetRole = target.getRole();

        validateHierarchyAccess(principal, target);
        return payrollRepo.findByEmployee_EmployeeId(employeeId)
                .stream()
                .map(this::map)
                .toList();
    }

    public List<PayrollResponse> getAll(CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if ((!principal.getRole().equals("ROLE_ADMIN") && (!principal.getRole().equals("ROLE_SUPER_ADMIN"))))  {
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
        String orgId=principal.getOrganizationId();

        if (role.equals("ROLE_SUPER_ADMIN")) {

            return payrollRepo.findAll()
                    .stream()
                    .map(this::map)
                    .toList();
        }

        if (role.equals("ROLE_HR")) {

            List<Employee> employees =
                    employeeRepository.findByOrganisationAndRole(orgId,Roles.ROLE_EMPLOYEE);

            List<String> ids = employees.stream()
                    .map(Employee::getEmployeeId)
                    .toList();

            return payrollRepo.findByEmployee_EmployeeIdIn(ids)
                    .stream()
                    .map(this::map)
                    .toList();
        }

        if (role.equals("ROLE_ADMIN")) {

            List<Employee> employees =
                    employeeRepository.findByOrganisationAndRoleIn(orgId,
                            List.of(
                                    "ROLE_HR",
                                    "ROLE_MANAGER",
                                    "ROLE_ACCOUNTANT",
                                    "ROLE_EMPLOYEE"
                            )
                    );

            List<String> ids = employees.stream()
                    .map(Employee::getEmployeeId)
                    .toList();

            return payrollRepo.findByEmployee_EmployeeIdIn(ids)
                    .stream()
                    .map(this::map)
                    .toList();
        }

        throw new ForbiddenException("Access denied");
    }
    /* ================= MAPPER ================= */

    private PayrollResponse map(Payroll p) {

        double payableDays =
                p.getPresentDays() + (p.getHalfDays() * 0.5);
        double tax = p.getProfessionalTax() == null ? 0 : p.getProfessionalTax();

        return PayrollResponse.builder()
                .id(p.getId())
                .employeeId(p.getEmployee().getEmployeeId())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .totalDays(p.getTotalDays())
                .presentDays(p.getPresentDays())
                .halfDays(p.getHalfDays())
                .absentDays(p.getAbsentDays())
                .payableDays(payableDays)
                .grossSalary(p.getGrossSalary())
                .pf(p.getTotalDeductions()) // optional adjust
                .professionalTax(tax)
                .netSalary(p.getNetSalary())
                .status(p.getStatus())
                .generatedAt(LocalDateTime.now())
                .generatedByRole(p.getGeneratedByRole())
                .build();
    }

    @Transactional
    public List<PayrollResponse> getPaidPayrolls(CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if ((!principal.getRole().equals("ROLE_ADMIN") && (!principal.getRole().equals("ROLE_SUPER_ADMIN"))))  {
            throw new ForbiddenException("Access denied");
        }
        String role = principal.getRole();
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot perform this action");
        }
        String orgId=principal.getOrganizationId();

        /* ================= SUPERADMIN / ADMIN / ACCOUNTANT ================= */

        if (role.equals("ROLE_SUPERADMIN") ||
                role.equals("ROLE_ACCOUNTANT")) {

            return payrollRepo.findByEmployee_OrganisationAndStatus(orgId,PayrollStatus.PAID)
                    .stream()
                    .map(this::map)
                    .toList();
        }
        if (role.equals("ROLE_ADMIN")) {

            List<Employee> employees =
                    employeeRepository.findByOrganisationAndRoleIn(orgId,List.of(
                            "ROLE_HR",
                            "ROLE_MANAGER",
                            "ROLE_ACCOUNTANT",
                            "ROLE_EMPLOYEE"
                    ));

            List<Long> employeeIds = employees.stream()
                    .map(Employee::getId)
                    .toList();

            return payrollRepo
                    .findByEmployee_IdInAndStatus(employeeIds, PayrollStatus.PAID)
                    .stream()
                    .map(this::map)
                    .toList();
        }

        /* ================= HR ================= */

        if (role.equals("ROLE_HR")) {

            List<Employee> employees =
                    employeeRepository.findByOrganisationAndRole(orgId,Roles.ROLE_EMPLOYEE);

            List<Long> employeeIds = employees.stream()
                    .map(Employee::getId)
                    .toList();

            return payrollRepo
                    .findByEmployee_IdInAndStatus(employeeIds, PayrollStatus.PAID)
                    .stream()
                    .map(this::map)
                    .toList();
        }

        /* ================= MANAGER ================= */

        throw new ForbiddenException("Access denied");
    }

    @Transactional
    public List<PayrollResponse> getPaidPayrollself(CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if (!principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")) {
            throw new ForbiddenException("Access denied");
        }

        String role = principal.getRole();
        UUID userId = UUID.fromString(principal.getUserId());

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get your profile.You are inactive user");

        return payrollRepo
                .findByEmployee_IdAndStatus(employee.getId(), PayrollStatus.PAID)
                .stream()
                .map(this::map)
                .toList();
    }

    public List<PayrollSummaryDto> getAllPayrollSummary(CustomUserPrincipal principal) {
        if (principal == null)
            throw new UnauthorizedException("Unauthorized");

        if (!principal.getRole().equals("ROLE_SUPER_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT"))
            throw new ForbiddenException("Access denied");
        String orgId = principal.getOrganizationId();

        List<Payroll> payrolls = payrollRepo.findByEmployee_OrganisationAndGeneratedAtIsNotNull(orgId);

        List<PayrollSummaryDto> result = new ArrayList<>();

        for (Payroll payroll : payrolls) {

            String empId = payroll.getEmployee().getEmployeeId();
            System.out.println(empId);
            SalaryStructure salary = salaryRepo
                    .findByEmployeeId(empId)
                    .orElseThrow(() -> new RuntimeException("Salary structure not found"));

            PayrollSummaryDto dto = new PayrollSummaryDto(
                    empId,
                    salary.getAnnualCtc(),
                    payroll.getMonth(),
                    payroll.getNetSalary()
            );

            result.add(dto);
        }

        return result;
    }


    // Endpoint 2 - Get payroll summaries by role
    public List<PayrollSummaryDto> getPayrollByRole(String role,CustomUserPrincipal principal) {
        if (principal == null)
            throw new UnauthorizedException("Unauthorized");

        if (!principal.getRole().equals("ROLE_SUPER_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT") && !principal.getRole().equals("ROLE_ACCOUNTANT"))
            throw new ForbiddenException("Access denied");
        Roles roleEnum = Roles.valueOf(role.toUpperCase());
        String orgId=principal.getOrganizationId();

        List<Payroll> payrolls = payrollRepo.findByEmployee_OrganisationAndGeneratedAtIsNotNull(orgId);

        List<PayrollSummaryDto> result = new ArrayList<>();

        for (Payroll payroll : payrolls) {

            if (payroll.getEmployee().getRole() == roleEnum) {

                String empId = payroll.getEmployee().getEmployeeId();

                SalaryStructure salary = salaryRepo
                        .findByEmployeeId(empId)
                        .orElseThrow(() -> new RuntimeException("Salary structure not found"));

                PayrollSummaryDto dto = new PayrollSummaryDto(
                        empId,
                        salary.getAnnualCtc(),
                        payroll.getMonth(),
                        payroll.getNetSalary()
                );

                result.add(dto);
            }
        }

        return result;
    }
    public List<PayrollSummaryDto> getPayrollSummaryByOrganization(
            String organisation,
            CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");

        if (!principal.getRole().equals("ROLE_SUPER_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT"))
            throw new ForbiddenException("Access denied");

        List<Employee> employees = employeeRepository.findByOrganisation(organisation);

        List<PayrollSummaryDto> result = new ArrayList<>();

        for (Employee employee : employees) {

            String empId = employee.getEmployeeId();

            List<Payroll> payrolls =
                    payrollRepo.findByEmployee_EmployeeId(empId);

            SalaryStructure salary =
                    salaryRepo.findByEmployeeId(empId)
                            .orElse(null);

            Double annualCtc = salary != null ? salary.getAnnualCtc() : null;

            for (Payroll payroll : payrolls) {

                if (payroll.getGeneratedAt() != null) {

                    PayrollSummaryDto dto = new PayrollSummaryDto(
                            empId,
                            annualCtc,
                            payroll.getMonth(),
                            payroll.getNetSalary()
                    );

                    result.add(dto);
                }
            }
        }

        return result;
    }

}