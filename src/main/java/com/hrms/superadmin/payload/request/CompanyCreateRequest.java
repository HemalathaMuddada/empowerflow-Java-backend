package com.hrms.superadmin.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyCreateRequest {

    @NotBlank(message = "Company name cannot be blank")
    @Size(max = 255, message = "Company name must be less than 255 characters")
    private String name;

    @Size(max = 1000, message = "Address must be less than 1000 characters")
    private String address; // Nullable
}
