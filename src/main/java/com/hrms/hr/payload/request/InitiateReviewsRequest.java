package com.hrms.hr.payload.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateReviewsRequest {

    @NotNull(message = "Review cycle ID cannot be null.")
    private Long reviewCycleId;

    @NotEmpty(message = "Employee IDs list cannot be empty.")
    private List<@NotNull(message = "Employee ID in list cannot be null.") Long> employeeIds;
}
