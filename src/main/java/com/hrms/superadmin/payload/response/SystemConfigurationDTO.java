package com.hrms.superadmin.payload.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigurationDTO {
    private String configKey;
    private String configValue;
    private String description;
    private String valueType;
    private LocalDateTime updatedAt;
    private String updatedByName; // Display name (e.g., username or full name) of the user who last updated
    private Long updatedById;     // ID of the user who last updated
}
