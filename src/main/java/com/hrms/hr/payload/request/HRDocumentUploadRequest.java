package com.hrms.hr.payload.request;

import com.hrms.employee.core.enums.DocumentType; // For validation if possible, or service layer
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRDocumentUploadRequest {

    @NotBlank(message = "Display name cannot be blank")
    @Size(max = 255, message = "Display name must be less than 255 characters")
    private String displayName;

    @NotBlank(message = "Document type cannot be blank")
    // Consider a custom validator to ensure this string matches one of DocumentType enum values
    private String documentType;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description; // Nullable

    @NotNull(message = "isGlobalPolicy flag must be provided")
    private Boolean isGlobalPolicy = false;
}
