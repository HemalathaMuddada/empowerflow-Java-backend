package com.hrms.employee.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceLogRequest {

    @NotNull(message = "Work date cannot be null")
    private LocalDate workDate;

    @NotNull(message = "Login time cannot be null")
    private LocalTime loginTime;

    private LocalTime logoutTime; // Nullable, can be logged later
}
