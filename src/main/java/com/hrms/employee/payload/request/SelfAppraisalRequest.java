package com.hrms.employee.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema; // Added

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for employee to submit their self-appraisal.")
public class SelfAppraisalRequest {

    @NotBlank(message = "Goals and Objectives cannot be blank.")
    @Size(max = 10000, message = "Goals and Objectives text is too long.")
    @Schema(description = "Goals and objectives discussed or set for the review period.", example = "Complete project X, Improve Y skill.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String goalsAndObjectives;

    @NotBlank(message = "Self-appraisal cannot be blank.")
    @Size(max = 20000, message = "Self-appraisal text is too long.")
    @Schema(description = "Employee's self-assessment of their performance.", example = "Met all targets for project X...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String employeeSelfAppraisal;
}
