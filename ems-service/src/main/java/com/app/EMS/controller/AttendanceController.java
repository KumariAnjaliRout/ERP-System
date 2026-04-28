package com.app.EMS.controller;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.AttendanceResponse;
import com.app.EMS.entity.Attendance;
import com.app.EMS.entity.AttendanceStatus;
import com.app.EMS.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 🔹 Employee Check-In
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','HR','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @PostMapping("/check-in")
    public ResponseEntity<String> checkIn(
            @AuthenticationPrincipal CustomUserPrincipal principal
            ) {
        attendanceService.checkIn(principal);
        return ResponseEntity.ok("Checked in successfully");
    }

    // 🔹 Employee Check-Out
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','HR','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @PostMapping("/check-out")
    public ResponseEntity<String> checkOut(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        attendanceService.checkOut(principal);
        return ResponseEntity.ok("Checked out successfully");
    }

    // 🔹 HR Manual Attendance
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/manual")
    public ResponseEntity<String> markByHR(
            @RequestParam String employeeId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam LocalDate date,
            @RequestParam AttendanceStatus status,
            @RequestParam Double noOfHoursWorked
    ){
        attendanceService.markAttendanceByHR(employeeId,principal,date,status,noOfHoursWorked);
        return ResponseEntity.ok("Attendance marked Successfully");
    }

    // 🔹 View Attendance
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceResponse>> getAttendance(
            @PathVariable String employeeId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                attendanceService.getEmployeeAttendancebyhr(employeeId,principal)
        );
    }
    // 🔹 Get ALL attendance records (Admin / HR view)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<AttendanceResponse>> getAllAttendance(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(
                attendanceService.getAllAttendance(principal)
        );
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getAttendanceByDate(
            @PathVariable LocalDate date,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                attendanceService.getAttendanceByDate(date,principal)
        );
    }
//    @PreAuthorize("hasAnyRole('EMPLOYEE','HR')")
//    @GetMapping("/employee")
//    public ResponseEntity<List<AttendanceResponse>> getAttendance(
//            @AuthenticationPrincipal CustomUserPrincipal principal
//    ) {
//        return ResponseEntity.ok(
//                attendanceService.getEmployeeAttendance(principal)
//        );
//    }
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','HR','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping("/my")
    public ResponseEntity<List<AttendanceResponse>> getMyAttendance(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                attendanceService.getMyAttendance(principal)
        );
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/monitor")
    public ResponseEntity<List<AttendanceResponse>> getRoleBasedAttendance(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                attendanceService.getRoleBasedAttendance(principal)
        );
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping("/self/date/{date}")
    public ResponseEntity<?> getSelfAttendance(
            @PathVariable LocalDate date,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ){
        return ResponseEntity.ok(
                attendanceService.getSelfAttendanceByDate(date, principal)
        );
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/monitor/date/{date}")
    public ResponseEntity<?> monitorAttendance(
            @PathVariable LocalDate date,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ){
        return ResponseEntity.ok(
                attendanceService.monitorAttendanceByDate(date, principal)
        );
    }
}

