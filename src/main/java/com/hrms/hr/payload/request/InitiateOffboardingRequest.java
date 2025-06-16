package com.hrms.hr.payload.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateOffboardingRequest {

    // @NotNull // Making it optional as per task description "optional but recommended"
    @FutureOrPresent(message = "Offboarding date must be today or in the future, if provided.")
    private LocalDate offboardingDate; // Optional

    @Size(max = 1000, message = "Reason for leaving must be less than 1000 characters.")
    private String reasonForLeaving; // Nullable

    @Size(max = 2000, message = "HR comments must be less than 2000 characters.")
    private String hrComments;       // Nullable
}
