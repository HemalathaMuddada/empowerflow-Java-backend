package com.hrms.employee.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkStatusReportRequest {

    @NotNull(message = "Report date cannot be null")
    private LocalDate reportDate;

    @NotBlank(message = "Tasks completed cannot be blank")
    private String tasksCompleted; // Potentially long text

    @NotBlank(message = "Tasks pending cannot be blank")
    private String tasksPending;   // Potentially long text

    private String blockers;       // Nullable, potentially long text
}
