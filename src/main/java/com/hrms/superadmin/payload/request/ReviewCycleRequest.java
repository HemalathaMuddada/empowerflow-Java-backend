package com.hrms.superadmin.payload.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCycleRequest {

    @NotBlank(message = "Review cycle name cannot be blank.")
    @Size(max = 100, message = "Name must be less than 100 characters.")
    private String name;

    @NotNull(message = "Start date cannot be null.")
    private LocalDate startDate;

    @NotNull(message = "End date cannot be null.")
    private LocalDate endDate;

    @NotBlank(message = "Status cannot be blank.")
    @Size(max = 50, message = "Status must be less than 50 characters.")
    // Consider an Enum for status if predefined values are strict, e.g., PLANNING, ACTIVE, COMPLETED, ARCHIVED
    private String status;

    @AssertTrue(message = "End date must be after or the same as start date.")
    private boolean isDatesChronological() {
        if (startDate == null || endDate == null) {
            return true; // Let @NotNull handle nulls, this check is for when both are present
        }
        return !endDate.isBefore(startDate);
    }
}
