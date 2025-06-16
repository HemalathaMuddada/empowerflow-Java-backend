package com.hrms.hr.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRFinalizeReviewRequest {

    @Size(max = 20000, message = "HR comments text is too long.")
    private String hrComments; // Nullable

    @Min(value = 1, message = "Final rating must be at least 1.")
    @Max(value = 5, message = "Final rating must be at most 5.") // Assuming a 1-5 scale
    private Integer finalRating; // Nullable, HR might only add comments without changing rating
}
