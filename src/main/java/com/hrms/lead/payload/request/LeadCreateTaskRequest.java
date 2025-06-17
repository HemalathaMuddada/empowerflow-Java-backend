package com.hrms.lead.payload.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadCreateTaskRequest {

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotBlank(message = "Description cannot be blank")
    private String description; // TEXT type

    @NotNull(message = "Deadline cannot be null")
    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;

    @NotNull(message = "Priority cannot be null")
    @Min(value = 0, message = "Priority must be at least 0") // Example: 0=Low, 1=Medium, 2=High
    @Max(value = 2, message = "Priority must be at most 2")
    private Integer priority;

    @NotNull(message = "Assigned to employee ID cannot be null")
    private Long assignedToEmployeeId;
}
