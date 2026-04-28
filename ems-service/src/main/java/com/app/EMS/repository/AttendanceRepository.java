package com.app.EMS.repository;
import com.app.EMS.entity.Attendance;
import com.app.EMS.entity.AttendanceStatus;
import com.app.EMS.entity.Roles;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployee_EmployeeIdAndDate(
            String employeeId,
            LocalDate date
    );
    Optional<Attendance> findByEmployee_UserIdAndDate(
            UUID userId,
            LocalDate date
    );
    boolean existsByEmployee_EmployeeIdAndDate(
            String employeeId,
            LocalDate date
    );
    boolean existsByEmployee_UserIdAndDate(
            UUID userId,
            LocalDate date
    );
    List<Attendance> findByEmployeeId_EmployeeId(String employeeId);
    List<Attendance> findByEmployeeId_UserId(UUID userid);
    List<Attendance> findByDate(LocalDate date);
    long countByDate(LocalDate date);

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);
    List<Attendance> findByEmployee_EmployeeIdAndDateBetween(
            String employeeId,
            LocalDate start,
            LocalDate end
    );
    long countByEmployee_IdInAndEmployee_OrganisationAndDateAndStatus(
            List<Long> employeeIds,
            String organisation,
            LocalDate date,
            AttendanceStatus status
    );
    long countByEmployee_IdInAndDateAndStatus(
            List<Long> employeeIds,
            LocalDate date,
            AttendanceStatus status
    );
    List<Attendance> findByEmployee_Role(Roles role);

    List<Attendance> findByEmployee_RoleIn(List<Roles> roles);
    List<Attendance> findByEmployee_OrganisationAndEmployee_Role(String organisation,Roles role);

    List<Attendance> findByEmployee_OrganisationAndEmployee_RoleIn(String organisation,List<Roles> roles);

    List<Attendance> findByEmployee_UserId(UUID userId);
    List<Attendance> findByDateAndEmployee_UserId(LocalDate date, UUID userId);

    List<Attendance> findByDateAndEmployee_Role(LocalDate date, Roles role);

    List<Attendance> findByDateAndEmployee_RoleIn(LocalDate date, List<Roles> roles);
    List<Attendance> findByDateAndEmployee_Organisation(LocalDate date, String organisation);
    List<Attendance> findByDateAndEmployee_OrganisationAndEmployee_Role(
            LocalDate date,
            String organisation,
            Roles role
    );

    List<Attendance> findByDateAndEmployee_OrganisationAndEmployee_RoleIn(
            LocalDate date,
            String organisation,
            List<Roles> roles
    );
}



