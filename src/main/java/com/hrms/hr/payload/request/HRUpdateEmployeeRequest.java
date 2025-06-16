package com.hrms.hr.payload.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRUpdateEmployeeRequest {

    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters if provided")
    private String firstName; // Optional

    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters if provided")
    private String lastName;  // Optional

    private LocalDate dateOfBirth; // Optional

    private Boolean isActive;      // Optional

    // To clear manager, client should send managerId as null if the field is present in JSON.
    // If managerId is not in JSON, it means "no change".
    // If managerId is present and null, it means "remove manager".
    // If managerId is present and non-null, it means "change manager".
    private Long managerId;        // Optional, can be null to remove manager

    private Long companyId;        // Optional
}
