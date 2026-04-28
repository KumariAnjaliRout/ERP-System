package com.app.EMS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "daily_attendance_summary",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAttendanceSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    private Long totalEmployees;

    private Long presentCount;

    private Long halfDayCount;

    private Long absentCount;
}
