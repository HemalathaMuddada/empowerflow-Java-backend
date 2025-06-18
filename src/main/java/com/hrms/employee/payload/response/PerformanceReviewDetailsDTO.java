package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema; // Added

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comprehensive details of a performance review.")
public class PerformanceReviewDetailsDTO {
    @Schema(description = "Unique ID of the performance review record.", example = "1")
    private Long id;

    @Schema(description = "Name of the review cycle.", example = "Annual Review 2023")
    private String reviewCycleName;

    @Schema(description = "Full name of the employee being reviewed.", example = "Jane Smith")
    private String employeeName;

    @Schema(description = "Full name of the manager/reviewer.", example = "Robert Brown")
    private String reviewerName;

    @Schema(description = "Goals and objectives for the review period.", example = "Achieve X, Develop Y.", nullable = true)
    private String goalsAndObjectives;

    @Schema(description = "Employee's self-appraisal comments.", example = "Exceeded targets in X...", nullable = true)
    private String employeeSelfAppraisal;

    @Schema(description = "Manager's evaluation comments.", example = "Consistently performs well...", nullable = true)
    private String managerEvaluation;

    @Schema(description = "Employee's final comments after manager and/or HR review.", example = "Agree with the feedback.", nullable = true)
    private String employeeComments;

    @Schema(description = "Overall rating given by the manager (e.g., 1-5).", example = "4", nullable = true)
    private Integer overallRatingByManager;

    @Schema(description = "Current status of the performance review.", example = "PENDING_EMPLOYEE_ACKNOWLEDGEMENT")
    private String status;

    @Schema(description = "Timestamp when employee submitted their self-appraisal.", nullable = true)
    private LocalDateTime submittedByEmployeeAt;

    @Schema(description = "Timestamp when manager submitted their evaluation.", nullable = true)
    private LocalDateTime reviewedByManagerAt;

    @Schema(description = "Timestamp when employee acknowledged the review.", nullable = true)
    private LocalDateTime acknowledgedByEmployeeAt;

    @Schema(description = "Timestamp when this review record was initiated/created.")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of the last update to this review record.")
    private LocalDateTime updatedAt;

    // Fields for HR review stage
    @Schema(description = "Comments from HR during the finalization step.", example = "Calibration adjustments made.", nullable = true)
    private String hrComments;

    @Schema(description = "Final overall rating after HR review/calibration (e.g., 1-5).", example = "4", nullable = true)
    private Integer finalRating;
}
