package com.hrms.superadmin.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigurationUpdateRequest {
    @NotBlank(message = "Configuration value cannot be blank.")
    @Size(max = 4000, message = "Configuration value is too long (max 4000 characters).") // Increased size limit slightly
    private String configValue;
}
