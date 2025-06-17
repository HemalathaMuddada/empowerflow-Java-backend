package com.hrms.superadmin.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema; // Added
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed information about a company.")
public class CompanyResponseDTO {
    @Schema(description = "Unique ID of the company.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Name of the company.", example = "Innovatech Solutions Ltd.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Physical address of the company.", example = "123 Innovation Drive, Tech City, TX 75001", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String address;

    @Schema(description = "Current active status of the company.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isActive;

    @Schema(description = "Timestamp of when the company record was created.", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of the last update to the company record.", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updatedAt;
}
