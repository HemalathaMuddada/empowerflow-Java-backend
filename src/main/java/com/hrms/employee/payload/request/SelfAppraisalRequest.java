package com.hrms.employee.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelfAppraisalRequest {

    @NotBlank(message = "Goals and Objectives cannot be blank.")
    @Size(max = 10000, message = "Goals and Objectives text is too long.") // Assuming TEXT column
    private String goalsAndObjectives;

    @NotBlank(message = "Self-appraisal cannot be blank.")
    @Size(max = 20000, message = "Self-appraisal text is too long.") // Assuming TEXT column
    private String employeeSelfAppraisal;
}
