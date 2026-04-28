package com.app.notification.service;

import com.app.notification.domain.enums.NotificationType;
import com.app.notification.domain.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
public class NotificationLinkBuilder {

    @Value("${frontend.base.url}")
    private String frontendBaseUrl;

    private static final Map<NotificationType, Map<Role, String>> ROUTE_MAP = new HashMap<>();

    static {
        /* ================= EMPLOYEE MANAGEMENT ================= */

        ROUTE_MAP.put(NotificationType.EMPLOYEE_CREATED, Map.of(
                Role.HR, "/AddEmployee",
                Role.ADMIN, "/_components/adminDashboard",
                Role.SUPER_ADMIN, "/admins"
        ));

        ROUTE_MAP.put(NotificationType.EMPLOYEE_DEACTIVATED, Map.of(
                Role.HR, "/employees",
                Role.ADMIN, "/_components/employeeDetails",
                Role.SUPER_ADMIN, "/admins"
        ));

        /* ================= LEAVE REQUEST FLOW ================= */

        ROUTE_MAP.put(NotificationType.LEAVE_REQUEST, Map.of(
                Role.HR, "/leave",
                Role.ADMIN, "/_components/leaves",
                Role.SUPER_ADMIN, "/orgleave"
        ));

        ROUTE_MAP.put(NotificationType.LEAVE_UPDATED, Map.of(
                Role.HR, "/leave",
                Role.ADMIN, "/_components/leaves",
                Role.SUPER_ADMIN, "/orgleave"
        ));

        ROUTE_MAP.put(NotificationType.LEAVE_CANCELLED, Map.of(
                Role.HR, "/leave",
                Role.ADMIN, "/_components/leaves",
                Role.SUPER_ADMIN, "/orgleave"
        ));

        /* ================= LEAVE DECISION ================= */
        ROUTE_MAP.put(NotificationType.LEAVE_APPROVED, Map.of(
                Role.EMPLOYEE, "/employeeLeave",
                Role.HR, "/my-leave",
                Role.ADMIN, "/leave/adminhistory",
                Role.MANAGER, "/leave/history",
                Role.ACCOUNTANT, "/leaves",
                Role.SUPER_ACCOUNTANT, "/leaves"
        ));

        ROUTE_MAP.put(NotificationType.LEAVE_REJECTED, Map.of(
                Role.EMPLOYEE, "/employeeLeave",
                Role.HR, "/my-leave",
                Role.ADMIN, "/leave/adminhistory",
                Role.MANAGER, "/leave/history",
                Role.ACCOUNTANT, "/leaves",
                Role.SUPER_ACCOUNTANT, "/leaves"
        ));

        /* ================= ATTENDANCE ================= */
        ROUTE_MAP.put(NotificationType.ATTENDANCE_MARKED, Map.of(
                Role.EMPLOYEE, "/employeeAttendance",
                Role.HR, "/my-attendance",
                Role.MANAGER, "/managerattendance",
                Role.ACCOUNTANT, "/attendance",
                Role.ADMIN, "/_components/manualattendance",
                Role.SUPER_ACCOUNTANT, "/attendance"
        ));

        /* ================= SALARY ================= */
        ROUTE_MAP.put(NotificationType.SALARY_STRUCTURE_CREATED, Map.of(
                Role.ADMIN, "/_components/salaryStructure",
                Role.HR, "/my-salary",
                Role.EMPLOYEE, "/employeeSalary",
                Role.MANAGER, "/salary",
                Role.ACCOUNTANT, "/mysalary",
                Role.SUPER_ACCOUNTANT,"/mysalary"
        ));

        ROUTE_MAP.put(NotificationType.SALARY_STRUCTURE_UPDATED, Map.of(
                Role.ADMIN, "/_components/salaryStructure"   // HR updated after rejection
        ));

        ROUTE_MAP.put(NotificationType.SALARY_STRUCTURE_APPROVED, Map.of(
                Role.HR, "/salary" ,
                Role.EMPLOYEE,"/employeeSalary"    // HR notified after approval
        ));

        ROUTE_MAP.put(NotificationType.SALARY_STRUCTURE_REJECTED, Map.of(
                Role.HR, "/salary"                           // HR needs to updated
        ));

        ROUTE_MAP.put(NotificationType.SALARY_STRUCTURE_DELETED, Map.of(
                Role.EMPLOYEE, "/employeeSalary"            // Employee salary removed
        ));

        ROUTE_MAP.put(NotificationType.SALARY_PAID, Map.of(
                Role.EMPLOYEE, "/employeeSalary",
                Role.HR, "/my-salary",
                Role.MANAGER, "/salary",
                Role.ACCOUNTANT, "/mysalary",
                Role.ADMIN, "/_components/mySalary",
                Role.SUPER_ACCOUNTANT, "/mysalary"
        ));

        ROUTE_MAP.put(NotificationType.PAYROLL_GENERATED, Map.of(
                Role.EMPLOYEE, "/employeeSalary",
                Role.HR, "/payroll",
                Role.MANAGER, "/salary",
                Role.ACCOUNTANT, "/mysalary",
                Role.ADMIN, "/_components/mySalary",
                Role.SUPER_ACCOUNTANT, "/mysalary"
        ));

        ROUTE_MAP.put(NotificationType.PAYSLIP_GENERATED, Map.of(
                Role.EMPLOYEE, "/employeePayslip",
                Role.HR, "/payslips",
                Role.MANAGER, "/salary",
                Role.ACCOUNTANT, "/mypayslip",
                Role.ADMIN, "/_components/mySalary",
                Role.SUPER_ACCOUNTANT, "/mypayslip"
        ));

        /* ================= ORDERS ================= */

        ROUTE_MAP.put(NotificationType.ORDER_CREATED, Map.of(
                Role.ADMIN, "/_components/ordersPage"
        ));
        ROUTE_MAP.put(NotificationType.ORDER_APPROVED, Map.of(
                Role.MANAGER, "/orders"
        ));
        ROUTE_MAP.put(NotificationType.ORDER_DELIVERED, Map.of(
                Role.MANAGER, "/orders"
        ));
        ROUTE_MAP.put(NotificationType.ORDER_REJECTED, Map.of(
                Role.OUTLET, "/myOrders"
        ));
        ROUTE_MAP.put(NotificationType.ORDER_DISPATCHED, Map.of(
                Role.OUTLET, "/myOrders"
        ));

        /* ================= TICKETS ================= */

        ROUTE_MAP.put(NotificationType.TICKET_CREATED, Map.of(
                Role.ADMIN, "/_components/adminSolveTicket",
                Role.SUPER_ADMIN, "/satickets"
        ));

        ROUTE_MAP.put(NotificationType.TICKET_STATUS_UPDATED, Map.of(
                Role.ADMIN, "/_components/adminticket",
                Role.HR, "/Components/ticket",
                Role.MANAGER, "/managerticket",
                Role.ACCOUNTANT, "/acctickets",
                Role.EMPLOYEE, "/employeeTicket",
                Role.SUPER_ACCOUNTANT,"/superacctickets",
                Role.OUTLET,"/outlettickets"
        ));

        ROUTE_MAP.put(NotificationType.TICKET_ESCALATED_TO_SUPER_ADMIN, Map.of(
                Role.SUPER_ADMIN, "/satickets"
        ));
    }

    public String build(NotificationType type, Role role) {

        Map<Role, String> roleRoutes = ROUTE_MAP.get(type);

        if (roleRoutes != null && roleRoutes.containsKey(role)) {
            return frontendBaseUrl + roleRoutes.get(role);
        }

        return frontendBaseUrl + roleDashboard(role);
    }

    private String roleDashboard(Role role) {
        return switch (role) {
            case ADMIN -> "/adminDashboard";
            case MANAGER -> "/managerDashboard";
            case HR -> "/hrDashboard";
            case EMPLOYEE -> "/employeeDashboard";
            case ACCOUNTANT -> "/Accdashboard";
            case SUPER_ACCOUNTANT -> "/SADashboard";
            case SUPER_ADMIN -> "/superAdmindashboard";
            case OUTLET -> "/outletDashboard";
        };
    }
}