//package com.app.EMS.service;
//
//import com.app.EMS.config.CustomUserPrincipal;
//import com.app.EMS.dto.EmployeeDashboardResponse;
//import com.app.EMS.entity.*;
//import com.app.EMS.exception.ForbiddenException;
//import com.app.EMS.exception.UnauthorizedException;
//import com.app.EMS.repository.*;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.data.domain.Sort;
//
//import java.time.LocalDate;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class AttendanceDashboardService {
//
//    private final AttendanceRepository attendanceRepository;
//    private final EmployeeRepository employeeRepository;
//    private final DailyAttendanceSummaryRepository summaryRepository;
//
//    @Transactional
//    public DailyAttendanceSummary generateDailySummary(LocalDate date, CustomUserPrincipal principal) {
//
//        if (principal == null) {
//            throw new UnauthorizedException("Unauthorized");
//        }
//        if (!principal.getRole().equals("ROLE_SUPER_ADMIN") ) {
//            throw new ForbiddenException("Access denied");
//        }
//
//        long totalEmployees = employeeRepository.count();
//
//        long present = attendanceRepository
//                .countByDateAndStatus(date, AttendanceStatus.PRESENT);
//
//        long halfDay = attendanceRepository
//                .countByDateAndStatus(date, AttendanceStatus.HALF_DAY);
//
//        long absent = totalEmployees - (present + halfDay);
//
//        DailyAttendanceSummary summary =
//                summaryRepository.findByDate(date)
//                        .orElse(new DailyAttendanceSummary());
//
//        summary.setDate(date);
//        summary.setTotalEmployees(totalEmployees);
//        summary.setPresentCount(present);
//        summary.setHalfDayCount(halfDay);
//        summary.setAbsentCount(absent);
//
//        return summaryRepository.save(summary);
//    }
//
//
//    public List<DailyAttendanceSummary> getAllSummaries(CustomUserPrincipal principal) {
//        if (principal == null) {
//            throw new UnauthorizedException("Unauthorized");
//        }
//        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")  ) {
//            throw new ForbiddenException("Access denied");
//        }
//        return summaryRepository.findAll(
//                Sort.by(Sort.Direction.DESC, "date")
//        );
//    }
//    public EmployeeDashboardResponse getEmployeeDashboard(CustomUserPrincipal principal) {
//        if (principal == null) {
//            throw new UnauthorizedException("Unauthorized");
//        }
//        if (!principal.getRole().equals("ROLE_SUPER_ADMIN") ) {
//            throw new ForbiddenException("Access denied");
//        }
//
//        long total = employeeRepository.count();
//        long active = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
//        long inactive = employeeRepository.countByStatus(EmployeeStatus.INACTIVE);
//
//        return EmployeeDashboardResponse.builder()
//                .totalEmployees(total)
//                .activeEmployees(active)
//                .inactiveEmployees(inactive)
//                .build();
//    }
//
//}

package com.app.EMS.service;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.EmployeeDashboardResponse;
import com.app.EMS.entity.*;
import com.app.EMS.exception.ForbiddenException;
import com.app.EMS.exception.UnauthorizedException;
import com.app.EMS.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceDashboardService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final DailyAttendanceSummaryRepository summaryRepository;


    /* =====================================================
                GET ACCESSIBLE EMPLOYEES
       ===================================================== */

    public List<Employee> getAccessibleEmployees(CustomUserPrincipal principal){

        String role = principal.getRole();
        String orgId=principal.getOrganizationId();

        // SUPER ADMIN → all employees
        if(role.equals("ROLE_SUPER_ADMIN")){
            return employeeRepository.findAll();
        }

        // ADMIN → HR, MANAGER, ACCOUNTANT
        if(role.equals("ROLE_ADMIN")){
            return employeeRepository.findByOrganisationAndRoleIn(orgId,
                    List.of("ROLE_HR",
                            "ROLE_MANAGER",
                            "ROLE_ACCOUNTANT",
                            "ROLE_EMPLOYEE"
                    )
            );
        }

        // HR → EMPLOYEES
        if(role.equals("ROLE_HR")){
            return employeeRepository.findByOrganisationAndRole(orgId,Roles.ROLE_EMPLOYEE);
        }

        throw new ForbiddenException("Access denied");
    }



    /* =====================================================
                GENERATE DAILY SUMMARY
       ===================================================== */

    //    @Transactional
//    public DailyAttendanceSummary generateDailySummary(LocalDate date,
//                                                       CustomUserPrincipal principal) {
//
//        if (principal == null) {
//            throw new UnauthorizedException("Unauthorized");
//        }
//
//        List<Employee> employees = getAccessibleEmployees(principal);
//
//        long totalEmployees = employees.size();
//        String orgId = principal.getOrganizationId();
//
//        List<Long> employeeIds = employees.stream()
//                .map(Employee::getId)
//                .toList();
//
//        long present = attendanceRepository
//                .countByEmployee_IdInAndEmployee_OrganisationAndDateAndStatus(
//                        employeeIds,
//                        orgId,
//                        date,
//                        AttendanceStatus.PRESENT
//                );
//
//        long halfDay = attendanceRepository
//                .countByEmployee_IdInAndEmployee_OrganisationAndDateAndStatus(
//                        employeeIds,
//                        orgId,
//                        date,
//                        AttendanceStatus.HALF_DAY
//                );
//
//        long absent = totalEmployees - (present + halfDay);
//
//        DailyAttendanceSummary summary =
//                summaryRepository.findByDate(date)
//                        .orElse(new DailyAttendanceSummary());
//
//        summary.setDate(date);
//        summary.setTotalEmployees(totalEmployees);
//        summary.setPresentCount(present);
//        summary.setHalfDayCount(halfDay);
//        summary.setAbsentCount(absent);
//
//        return summaryRepository.save(summary);
//    }
    @Transactional
    public DailyAttendanceSummary generateDailySummary(
            LocalDate date,
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        String role = principal.getRole();
        String orgId = principal.getOrganizationId();

        List<Employee> employees = getAccessibleEmployees(principal);

        long totalEmployees = employees.size();

        List<Long> employeeIds;
        long present;
        long halfDay;

        if (role.equals("ROLE_SUPER_ADMIN")) {

            // 🔓 All employees
            employeeIds = employees.stream()
                    .map(Employee::getId)
                    .toList();
            present = attendanceRepository
                    .countByEmployee_IdInAndDateAndStatus(
                            employeeIds,
                            date,
                            AttendanceStatus.PRESENT
                    );

            halfDay = attendanceRepository
                    .countByEmployee_IdInAndDateAndStatus(
                            employeeIds,
                            date,
                            AttendanceStatus.HALF_DAY
                    );
        }

        else if (role.equals("ROLE_HR")) {

            // 🔐 Only EMPLOYEE role
            employeeIds = employees.stream()
                    .filter(emp -> emp.getRole().equals("ROLE_EMPLOYEE"))
                    .map(Employee::getId)
                    .toList();
            present = attendanceRepository
                    .countByEmployee_IdInAndEmployee_OrganisationAndDateAndStatus(
                            employeeIds,
                            orgId,
                            date,
                            AttendanceStatus.PRESENT
                    );

            halfDay = attendanceRepository
                    .countByEmployee_IdInAndEmployee_OrganisationAndDateAndStatus(
                            employeeIds,
                            orgId,
                            date,
                            AttendanceStatus.HALF_DAY
                    );
        }

        else if (role.equals("ROLE_ADMIN")) {

            // 🔐 HR + MANAGER + ACCOUNTANT + EMPLOYEE
            employeeIds = employees.stream()
                    .filter(emp ->
                            emp.getRole().equals("ROLE_HR") ||
                                    emp.getRole().equals("ROLE_MANAGER") ||
                                    emp.getRole().equals("ROLE_ACCOUNTANT") ||
                                    emp.getRole().equals("ROLE_EMPLOYEE")
                    )
                    .map(Employee::getId)
                    .toList();
            present = attendanceRepository
                    .countByEmployee_IdInAndEmployee_OrganisationAndDateAndStatus(
                            employeeIds,
                            orgId,
                            date,
                            AttendanceStatus.PRESENT
                    );

            halfDay = attendanceRepository
                    .countByEmployee_IdInAndEmployee_OrganisationAndDateAndStatus(
                            employeeIds,
                            orgId,
                            date,
                            AttendanceStatus.HALF_DAY
                    );
        }

        else {
            throw new ForbiddenException("Access denied");
        }

        long absent = totalEmployees - (present + halfDay);

        DailyAttendanceSummary summary =
                summaryRepository.findByDate(date)
                        .orElse(new DailyAttendanceSummary());

        summary.setDate(date);
        summary.setTotalEmployees(totalEmployees);
        summary.setPresentCount(present);
        summary.setHalfDayCount(halfDay);
        summary.setAbsentCount(absent);

        return summaryRepository.save(summary);
    }



    /* =====================================================
                GET SUMMARIES
       ===================================================== */

    public List<DailyAttendanceSummary> getAllSummaries(CustomUserPrincipal principal) {

        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        getAccessibleEmployees(principal); // validate access

        return summaryRepository.findAll(
                Sort.by(Sort.Direction.DESC, "date")
        );
    }



    /* =====================================================
                EMPLOYEE DASHBOARD
       ===================================================== */

    public EmployeeDashboardResponse getEmployeeDashboard(CustomUserPrincipal principal) {

        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        List<Employee> employees = getAccessibleEmployees(principal);

        long total = employees.size();

        long active = employees.stream()
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE)
                .count();

        long inactive = employees.stream()
                .filter(e -> e.getStatus() == EmployeeStatus.INACTIVE)
                .count();

        return EmployeeDashboardResponse.builder()
                .totalEmployees(total)
                .activeEmployees(active)
                .inactiveEmployees(inactive)
                .build();
    }

}