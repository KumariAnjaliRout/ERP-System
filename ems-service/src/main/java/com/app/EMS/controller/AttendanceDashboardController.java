package com.app.EMS.controller;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.EmployeeDashboardResponse;
import com.app.EMS.entity.DailyAttendanceSummary;
import com.app.EMS.entity.Employee;
import com.app.EMS.service.AttendanceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController

@RequestMapping("/api/dashboard/attendance")
@RequiredArgsConstructor
public class AttendanceDashboardController {

    private final AttendanceDashboardService dashboardService;
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR')")
    // 🔹 Generate / Update summary (HR / Scheduler)
    @GetMapping("/generate")
    public ResponseEntity<DailyAttendanceSummary> generate(
            @RequestParam LocalDate date,
            @AuthenticationPrincipal CustomUserPrincipal  principal
            ) {
        return ResponseEntity.ok(
                dashboardService.generateDailySummary(date,principal)
        );
    }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR')")
    @GetMapping("/all")
    public ResponseEntity<List<DailyAttendanceSummary>> getAllSummaries(@AuthenticationPrincipal CustomUserPrincipal  principal) {
        return ResponseEntity.ok(
                dashboardService.getAllSummaries(principal)
        );
    }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR')")
    @GetMapping("/employee")
    public ResponseEntity<EmployeeDashboardResponse> getDashboard(@AuthenticationPrincipal CustomUserPrincipal  principal) {
        return ResponseEntity.ok(
                dashboardService.getEmployeeDashboard(principal)
        );
    }
}