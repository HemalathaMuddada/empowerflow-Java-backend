package com.hrms.employee.payload.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationRequest {

    @NotBlank(message = "Leave type cannot be blank")
    private String leaveType; // Will be mapped to LeaveType enum

    @NotNull(message = "Start date cannot be null")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date cannot be null")
    private LocalDate endDate;

    @NotBlank(message = "Reason cannot be blank")
    private String reason;

    // Custom validation for endDate after startDate can be added at the class level
    // or handled in the service layer. For now, basic checks are here.
}
