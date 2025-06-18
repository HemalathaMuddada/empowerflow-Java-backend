package com.hrms.superadmin.payload.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema; // Added
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to create a new system configuration parameter.")
public class SystemConfigurationCreateRequest {
    @NotBlank(message = "Configuration key cannot be blank.")
    @Size(max = 100, message = "Configuration key must be 100 characters or less.")
    @Pattern(regexp = "^[A-Z0-9_.]+$", message = "Configuration key can only contain uppercase letters, numbers, underscores, and periods.")
    @Schema(description = "Unique key for the configuration (e.g., MIN_WORK_HOURS).", example = "FEATURE_FLAG_X", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configKey;

    @NotBlank(message = "Configuration value cannot be blank.")
    @Size(max = 4000, message = "Configuration value is too long (max 4000 characters).")
    @Schema(description = "Value of the configuration parameter.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configValue;

    @Size(max = 1000, message = "Description is too long (max 1000 characters).")
    @Schema(description = "Description of the configuration parameter.", example = "Enables experimental feature X.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description; // Optional

    @NotBlank(message = "Value type cannot be blank.")
    @Pattern(regexp = "NUMBER|STRING|TIME|BOOLEAN|JSON", message = "Invalid value type. Allowed: NUMBER, STRING, TIME, BOOLEAN, JSON.")
    @Schema(description = "Data type of the value (NUMBER, STRING, TIME, BOOLEAN, JSON).", example = "BOOLEAN", requiredMode = Schema.RequiredMode.REQUIRED)
    private String valueType;
}
