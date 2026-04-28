package com.app.EMS.repository;

import com.app.EMS.entity.EmployeeLeave;
import com.app.EMS.entity.LeaveStatus;
import com.app.EMS.entity.LeaveType;
import com.app.EMS.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeLeaveRepository
        extends JpaRepository<EmployeeLeave, Long> {

    List<EmployeeLeave> findByEmployee_EmployeeId(String employeeId);
    List<EmployeeLeave> findByEmployee_UserId(UUID userId);
    List<EmployeeLeave> findByStatus(LeaveStatus status);
    Optional<EmployeeLeave> findByIdAndEmployee_UserId(Long id, UUID UserId);

    List<EmployeeLeave> findByEmployee_Role(Roles role);

    List<EmployeeLeave> findByEmployee_RoleIn(List<Roles> roles);
    List<EmployeeLeave> findByStatusAndEmployee_Role(
            LeaveStatus status, Roles role);

    List<EmployeeLeave> findByStatusAndEmployee_RoleIn(
            LeaveStatus status, List<Roles> roles);
    boolean existsByEmployee_UserIdAndStartDateAndEndDateAndLeaveType(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            LeaveType leaveType
    );
    List<EmployeeLeave> findByEmployee_EmployeeIdAndStatusAndLeaveTypeAndStartDateBetween(
            String employeeId,
            LeaveStatus status,
            LeaveType leaveType,
            LocalDate start,
            LocalDate end
    );
    List<EmployeeLeave> findByStatusAndEmployee_OrganisationAndEmployee_Role(
            LeaveStatus status,String Organisation,Roles role);

    List<EmployeeLeave> findByStatusAndEmployee_OrganisationAndEmployee_RoleIn(
            LeaveStatus status,String Organisation,List<Roles> roles);
    List<EmployeeLeave> findByEmployee_OrganisationAndEmployee_Role(String organisation,Roles role);

    List<EmployeeLeave> findByEmployee_OrganisationAndEmployee_RoleIn(String organisation,List<Roles> roles);
}

