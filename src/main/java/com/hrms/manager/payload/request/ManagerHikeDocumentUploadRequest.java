package com.hrms.manager.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerHikeDocumentUploadRequest {

    @NotNull(message = "Employee ID cannot be null")
    private Long employeeId;

    @NotBlank(message = "Display name cannot be blank")
    @Size(max = 255, message = "Display name must be less than 255 characters")
    private String displayName;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description; // Nullable
}
