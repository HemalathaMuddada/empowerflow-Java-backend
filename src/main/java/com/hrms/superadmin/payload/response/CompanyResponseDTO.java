package com.hrms.superadmin.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponseDTO {
    private Long id;
    private String name;
    private String address; // Nullable
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
