package com.app.EMS.controller;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.LeaveActionRequest;
import com.app.EMS.dto.LeaveApplyRequest;
import com.app.EMS.dto.LeaveResponse;
import com.app.EMS.entity.LeaveStatus;
import com.app.EMS.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // Employee applies leave
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @PostMapping("/apply")
    public ResponseEntity<String> applyLeave(
            @RequestBody @Valid LeaveApplyRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        leaveService.applyLeave(request,principal);
        return ResponseEntity.ok("Leave applied successfully");
    }
    // Employee views own leaves
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @GetMapping("/my")
    public ResponseEntity<?> myLeaves(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                leaveService.getMyLeaves(principal)
        );
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> myLeavesbyhr(@PathVariable String employeeId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                leaveService.getLeavesByEmployeeForMonitor(employeeId,principal)
        );
    }
    // HR approves / rejects
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/action/{leaveId}")
    public ResponseEntity<String> actionLeave(
            @PathVariable Long leaveId,
            @RequestBody @Valid LeaveActionRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        leaveService.actionLeave(leaveId, request,principal);
        return ResponseEntity.ok("Leave updated successfully");
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getLeavesByStatus(
            @PathVariable LeaveStatus status,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                leaveService.getLeavesByStatus(status,principal)
        );
    }
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<LeaveResponse>> getAllLeaves(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(
                leaveService.getAllLeaves(principal)
        );
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long id,
                                         @AuthenticationPrincipal CustomUserPrincipal principal) {
        leaveService.deleteLeave(id,principal);
        return ResponseEntity.ok("Leave deleted successfully");
    }
    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @GetMapping("/monitor")
    public ResponseEntity<List<LeaveResponse>> monitorLeaves(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                leaveService.monitorLeaves(principal)
        );
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','ACCOUNTANT','ADMIN','SUPER_ACCOUNTANT')")
    @PutMapping("/update/{leaveId}")
    public ResponseEntity<String> updateLeave(
            @PathVariable Long leaveId,
            @RequestBody LeaveApplyRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        leaveService.updateLeaveRequest(leaveId, request, principal);
        return ResponseEntity.ok("Leave request updated successfully");
    }
}
