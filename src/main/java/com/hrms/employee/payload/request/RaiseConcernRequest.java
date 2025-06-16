package com.hrms.employee.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RaiseConcernRequest {

    @NotBlank(message = "Concern text cannot be blank")
    private String concernText;

    private String category; // e.g., "Workload", "Interpersonal", "Facilities", "Other"

    private String targetRole; // e.g., "HR", "LEAD", "MANAGER" - for initial categorization hint
}
