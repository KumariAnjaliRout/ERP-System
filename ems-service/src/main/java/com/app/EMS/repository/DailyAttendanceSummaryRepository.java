package com.app.EMS.repository;

import com.app.EMS.entity.AttendanceStatus;
import com.app.EMS.entity.DailyAttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyAttendanceSummaryRepository
        extends JpaRepository<DailyAttendanceSummary, Long> {

    Optional<DailyAttendanceSummary> findByDate(LocalDate date);

}
