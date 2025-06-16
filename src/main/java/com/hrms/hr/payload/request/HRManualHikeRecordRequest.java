package com.hrms.hr.payload.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRManualHikeRecordRequest {

    @NotNull(message = "Employee ID cannot be null")
    private Long employeeId;

    @NotNull(message = "Old salary cannot be null")
    @PositiveOrZero(message = "Old salary must be positive or zero")
    private BigDecimal oldSalary;

    @PositiveOrZero(message = "Hike percentage must be positive or zero if provided")
    private BigDecimal hikePercentage; // Optional

    @PositiveOrZero(message = "Hike amount must be positive or zero if provided")
    private BigDecimal hikeAmount;     // Optional

    @NotNull(message = "Effective date cannot be null")
    private LocalDate effectiveDate;

    @Size(max = 255, message = "Promotion title must be less than 255 characters")
    private String promotionTitle; // Nullable

    @Size(max = 1000, message = "Comments must be less than 1000 characters")
    private String comments;       // Nullable

    @AssertTrue(message = "Either hikePercentage or hikeAmount (or both) must be provided and non-negative.")
    private boolean isHikeValueProvided() {
        boolean percentageProvided = (hikePercentage != null && hikePercentage.compareTo(BigDecimal.ZERO) >= 0);
        boolean amountProvided = (hikeAmount != null && hikeAmount.compareTo(BigDecimal.ZERO) >= 0);
        return percentageProvided || amountProvided;
    }
}
