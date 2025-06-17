package com.hrms.superadmin.payload.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyUpdateRequest {

    @Size(max = 255, message = "Company name must be less than 255 characters if provided")
    private String name; // Optional

    @Size(max = 1000, message = "Address must be less than 1000 characters if provided")
    private String address; // Optional

    private Boolean isActive; // Optional
}
