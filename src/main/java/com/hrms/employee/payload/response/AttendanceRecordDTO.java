package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordDTO {
    private Long id;
    private LocalDate workDate;
    private LocalDateTime loginTime;    // Combined Date and Time
    private LocalDateTime logoutTime;   // Combined Date and Time, nullable
    private Double totalHours;          // Nullable
    private boolean isRegularized;
    private String notes;               // Nullable
}
