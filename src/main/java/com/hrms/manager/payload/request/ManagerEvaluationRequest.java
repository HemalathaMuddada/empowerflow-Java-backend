package com.hrms.manager.payload.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerEvaluationRequest {

    @NotBlank(message = "Manager evaluation cannot be blank.")
    @Size(max = 20000, message = "Manager evaluation text is too long.") // Assuming TEXT column
    private String managerEvaluation;

    @NotNull(message = "Overall rating by manager cannot be null.")
    @Min(value = 1, message = "Rating must be at least 1.")
    @Max(value = 5, message = "Rating must be at most 5.") // Assuming a 1-5 scale
    private Integer overallRatingByManager;
}
