package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentListItemDTO {
    private Long id;
    private String displayName;
    private String documentType; // From DocumentType enum
    private String description;  // Nullable
    private LocalDateTime uploadedAt; // From createdAt of Document entity
    private String uploaderName;   // Nullable, from uploadedBy user if present
    private String scope;          // e.g., "Personal", "Company", "Global"
}
