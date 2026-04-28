package com.app.EMS.service;

import com.app.EMS.client.NotificationFeignClient;
import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.AttendanceResponse;
import com.app.EMS.dto.NotificationRequestDto;
import com.app.EMS.entity.*;
import com.app.EMS.exception.*;
import com.app.EMS.repository.AttendanceRepository;
import com.app.EMS.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationFeignClient notificationFeignClient;

    private void validateHierarchyAccess(CustomUserPrincipal principal, Employee targetEmployee) {

        String loggedRole = principal.getRole();
        Roles targetRole = targetEmployee.getRole();
        UUID loggedUserId = UUID.fromString(principal.getUserId());

        // SUPER ADMIN → unrestricted
        if (loggedRole.equals("ROLE_SUPER_ADMIN")) {
            return;
        }

        // ADMIN → HR, MANAGER, ACCOUNTANT + own
        if (loggedRole.equals("ROLE_ADMIN")) {

            if (targetEmployee.getUserId().equals(loggedUserId)) {
                return;
            }

            if (targetRole == Roles.ROLE_HR ||
                    targetRole == Roles.ROLE_MANAGER ||
                    targetRole == Roles.ROLE_ACCOUNTANT ||
                    targetRole == Roles.ROLE_EMPLOYEE) {
                return;
            }

            throw new ForbiddenException("Admin cannot access this role");
        }

        // HR → EMPLOYEE + own
        if (loggedRole.equals("ROLE_HR")) {

            if (targetEmployee.getUserId().equals(loggedUserId)) {
                return;
            }

            if (targetRole == Roles.ROLE_EMPLOYEE) {
                return;
            }

            throw new ForbiddenException("HR can access only employee data");
        }

        // Remaining roles → own data only
        if (!targetEmployee.getUserId().equals(loggedUserId)) {
            throw new ForbiddenException("You can access only your own data");
        }
    }
    // ✅ Employee Check-In
    @Transactional
    public void checkIn(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_HR")
                && !principal.getRole().equals("ROLE_MANAGER") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());

        String role= principal.getRole();
        LocalDate today = LocalDate.now();
        if (attendanceRepository
                .existsByEmployee_UserIdAndDate(userid, today)) {
            throw new AlreadyExistsResourceException("Attendance already marked for today");
        }
        Employee employee = employeeRepository.findByUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get your profile.You are inactive user");

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(today);
        attendance.setCheckIn(LocalTime.now());
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setMarkedBy(MarkedBy.valueOf(role.replace("ROLE_", "")));
        attendance.setRole(Roles.valueOf(role));

        attendanceRepository.save(attendance);
    }

    // ✅ Employee Check-Out
    @Transactional
    public void checkOut(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_HR")
                && !principal.getRole().equals("ROLE_MANAGER") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")) {
            throw new ForbiddenException("Access denied");
        }

        UUID userid= UUID.fromString(principal.getUserId());
        Employee employee = employeeRepository.findByUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get your profile.You are inactive user");


        Attendance attendance = attendanceRepository
                .findByEmployee_UserIdAndDate(userid, LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("Check-in not found"));

        if (attendance.getCheckOut() != null) {
            throw new AlreadyExistsResourceException("Already checked out");
        }

        attendance.setCheckOut(LocalTime.now());

        double hoursWorked = Duration.between(
                attendance.getCheckIn(),
                attendance.getCheckOut()
        ).toHours();
        attendance.setNoOfHoursWorked(hoursWorked);
        attendance.setStatus(
                hoursWorked < 4
                        ? AttendanceStatus.HALF_DAY
                        : AttendanceStatus.PRESENT
        );

        attendanceRepository.save(attendance);
    }

    @Transactional
    public void markAttendanceByHR(
            String employeeId,
            CustomUserPrincipal principal,
            LocalDate date,
            AttendanceStatus status,
            Double noOfHoursWorked
    ) {
        String role= principal.getRole();
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN") &&
                !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        Employee employeee = employeeRepository.findByUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot mark attendance for other users.You are inactive user");
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            throw new BadRequestException(
                    "Future attendance cannot be marked"
            );
        }
        if (attendanceRepository
                .existsByEmployee_EmployeeIdAndDate(employeeId,date)) {
            throw new AlreadyExistsResourceException("Attendance already marked for mentioned date");
        }
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot mark attendance for user in another organization");
            }
        }
        if (principal.getRole().equals("ROLE_HR")
                && employee.getRole() == Roles.ROLE_HR) {
            throw new BadRequestException("HR can only mark attendance for EMPLOYEE");
        }
        if (principal.getRole().equals("ROLE_HR")
                && employee.getRole() == Roles.ROLE_ACCOUNTANT) {
            throw new BadRequestException("HR cannot mark attendance for ACCOUNTANT");
        }
        if (principal.getRole().equals("ROLE_HR")
                && employee.getRole() != Roles.ROLE_EMPLOYEE) {
            throw new BadRequestException("HR can mark attendance only for EMPLOYEE");
        }
        if (principal.getRole().equals("ROLE_ADMIN")
                && employee.getRole()!= Roles.ROLE_HR && employee.getRole()!= Roles.ROLE_ACCOUNTANT
                && employee.getRole()!= Roles.ROLE_MANAGER && employee.getRole()!= Roles.ROLE_EMPLOYEE) {
            throw new BadRequestException("Admin mark attendance for HR,MANAGER,EMPLOYEE and ACCOUNTANT roles");
        }
//        if (principal.getRole().equals("ROLE_SUPER_ADMIN")
//                && employee.getRole() != Roles.ROLE_ADMIN && employee.getRole() != Roles.ROLE_ACCOUNTANT) {
//            throw new BadRequestException("Admin cannot mark attendance for EMPLOYEE");
//        }
        Attendance attendance = attendanceRepository
                .findByEmployee_EmployeeIdAndDate(employeeId, date)
                .orElse(new Attendance());
        attendance.setEmployee(employee);
        attendance.setDate(date);
        attendance.setStatus(status);
        attendance.setNoOfHoursWorked(noOfHoursWorked);
        attendance.setRole(employee.getRole());
        attendance.setMarkedBy(MarkedBy.valueOf((role).replace("ROLE_","")));
        attendanceRepository.save(attendance);
        NotificationRequestDto.NotificationRequestDtoBuilder builder =
                NotificationRequestDto.builder()
                        .category(NotificationCategory.ATTENDANCE)
                        .type(NotificationType.ATTENDANCE_MARKED)
                        .priority(NotificationPriority.NORMAL)
                        .organizationId(principal.getOrganizationId())
                        .targetUserId(employee.getUserId())   // always required
                        .targetRole(employee.getRole().name().replace("ROLE_", ""))
                        .metadata(Map.of(
                                "triggeredByRole", principal.getRole(),
                                "triggeredByUserId", principal.getUserId(),
                                "employeeId", employee.getEmployeeId(),
                                "date", attendance.getDate().toString()
                        ));

        notificationFeignClient.sendNotification(builder.build());
    }
    public List<AttendanceResponse> getEmployeeAttendancebyhr(String employeeId,CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        Employee employeee = employeeRepository.findByUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get attendance for other users.You are inactive user");
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot mark attendance for user in another organization");
            }
        }
//        if (principal.getRole().equals("ROLE_HR")
//                && employee.getRole() == Roles.ROLE_HR) {
//            throw new ForbiddenException("HR can only see Employee details");
//        }
//        if (principal.getRole().equals("ROLE_HR")
//                && employee.getRole() == Roles.ROLE_MANAGER) {
//            throw new ForbiddenException("HR cannot view details of MANAGER");
//        }
//        if (principal.getRole().equals("ROLE_HR")
//                && employee.getRole() != Roles.ROLE_EMPLOYEE) {
//            throw new BadRequestException("HR can see attendance only for EMPLOYEE");
//        }
        validateHierarchyAccess(principal, employee);
        return attendanceRepository.findByEmployeeId_EmployeeId(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public List<AttendanceResponse> getAllAttendance(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        return attendanceRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    //    private Double calculateHoursWorked(LocalTime checkIn, LocalTime checkOut) {
//        if (checkIn == null || checkOut == null) {
//            return attendance.getNoOfHoursWorked();
//        }
//        return Duration.between(checkIn, checkOut).toMinutes() / 60.0;
//    }
    private Double resolveHoursWorked(Attendance attendance) {

        // HR manual entry → use stored value
        if (attendance.getNoOfHoursWorked() != null) {
            return attendance.getNoOfHoursWorked();
        }

        // Employee check-in/out → calculate
        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {
            return Duration.between(
                    attendance.getCheckIn(),
                    attendance.getCheckOut()
            ).toMinutes() / 60.0;
        }

        // Nothing available
        return 0.0;
    }
    public List<AttendanceResponse> getMyAttendance(CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if (!principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_HR")
                && !principal.getRole().equals("ROLE_MANAGER") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        Employee employee = employeeRepository.findByUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get your profile.You are inactive user");
        return attendanceRepository
                .findByEmployee_UserId(userid)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public List<AttendanceResponse> getRoleBasedAttendance(CustomUserPrincipal principal) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        Employee employeee = employeeRepository.findByUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get attendance for other users.You are inactive user");
        String role = principal.getRole();
        List<Attendance> list;
        String orgId=principal.getOrganizationId();


        switch (role) {
            case "ROLE_SUPER_ADMIN":
                list = attendanceRepository.findAll();
                break;

            case "ROLE_HR":

                list = attendanceRepository.findByEmployee_OrganisationAndEmployee_Role(
                        orgId,
                        Roles.ROLE_EMPLOYEE);
                break;

            case "ROLE_ADMIN":

                list = attendanceRepository.findByEmployee_OrganisationAndEmployee_RoleIn(
                        orgId,
                        List.of(Roles.ROLE_HR, Roles.ROLE_MANAGER, Roles.ROLE_ACCOUNTANT, Roles.ROLE_EMPLOYEE)
                );
                break;

            default:
                throw new ForbiddenException("Access denied");
        }

        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }
    private AttendanceResponse mapToResponse(Attendance attendance) {
        Employee e = attendance.getEmployee();

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .employeeId(e.getEmployeeId())
                .firstname(e.getFirstName())
                .lastname(e.getLastName())
                .date(attendance.getDate())
                .checkIn(attendance.getCheckIn())

                .checkOut(attendance.getCheckOut())
//                .noOfHoursWorked(calculateHoursWorked(
//                        attendance.getCheckIn(),
//                        attendance.getCheckOut()
//                ))
                .noOfHoursWorked(resolveHoursWorked(attendance))
                .status(attendance.getStatus())
                .markedBy(attendance.getMarkedBy())
                .role(attendance.getRole())
                .build();
    }
    @Transactional
    public List<AttendanceResponse> getAttendanceByDate(LocalDate date,CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR")  && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        Employee employeee = employeeRepository.findByUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get attendance for other users.You are inactive user");

        return attendanceRepository.findByDateAndEmployee_Organisation(date, principal.getOrganizationId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Transactional
    public List<AttendanceResponse> getSelfAttendanceByDate(
            LocalDate date,
            CustomUserPrincipal principal
    ) {
        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_EMPLOYEE")
                &&  !principal.getRole().equals("ROLE_MANAGER") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        Employee employeee = employeeRepository.findByUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get your attendance users.You are inactive user");

        List<Attendance> list =
                attendanceRepository
                        .findByDateAndEmployee_UserId(date, userid);

        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Transactional
    public List<AttendanceResponse> monitorAttendanceByDate(
            LocalDate date,
            CustomUserPrincipal principal
    ) {

        if (principal == null)
            throw new UnauthorizedException("Unauthorized");
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userid= UUID.fromString(principal.getUserId());
        Employee employeee = employeeRepository.findByUserId(userid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userid, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get attendance for other users.You are inactive user");

        String role = principal.getRole();
        String orgId = principal.getOrganizationId();

        List<Attendance> list;

        switch (role) {
            case "ROLE_SUPER_ADMIN":

                list = attendanceRepository.findByDate(date);
                break;

            case "ROLE_HR":

                list = attendanceRepository
                        .findByDateAndEmployee_OrganisationAndEmployee_Role(date,orgId,Roles.ROLE_EMPLOYEE);
                break;

            case "ROLE_ADMIN":

                list = attendanceRepository
                        .findByDateAndEmployee_OrganisationAndEmployee_RoleIn(
                                date,orgId,
                                List.of(Roles.ROLE_HR, Roles.ROLE_MANAGER,Roles.ROLE_ACCOUNTANT,Roles.ROLE_EMPLOYEE)
                        );
                break;

            default:
                throw new ForbiddenException(
                        "You are not allowed to monitor attendance"
                );
        }
        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }
}


