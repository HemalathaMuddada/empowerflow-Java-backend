package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReviewDetailsDTO {
    private Long id;
    private String reviewCycleName;
    private String employeeName; // Should be own name for employee view
    private String reviewerName; // Manager's name

    private String goalsAndObjectives;
    private String employeeSelfAppraisal;
    private String managerEvaluation; // Nullable, visible after manager review
    private String employeeComments;    // Nullable, for after manager review

    private Integer overallRatingByManager; // Nullable
    // private Integer finalRating; // Typically not shown to employee until fully finalized by HR. Omitting for now.

    private String status;
    private LocalDateTime submittedByEmployeeAt;
    private LocalDateTime reviewedByManagerAt;
    private LocalDateTime acknowledgedByEmployeeAt;
    private LocalDateTime createdAt; // Review initiation date
    private LocalDateTime updatedAt; // Last update to the review record

    // Fields for HR review stage
    private String hrComments; // Nullable
    private Integer finalRating; // Nullable
}
