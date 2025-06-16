package com.hrms.hr.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRUpdateDesignationRequest {

    @NotBlank(message = "New designation cannot be blank")
    @Size(max = 255, message = "Designation must be less than 255 characters")
    private String newDesignation;
}
