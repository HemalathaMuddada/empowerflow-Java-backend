package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkStatusReportResponse {
    private Long id;
    private Long employeeId;
    private LocalDate reportDate;
    private String tasksCompleted;
    private String tasksPending;
    private String blockers;
    private LocalDateTime submittedAt;
}
