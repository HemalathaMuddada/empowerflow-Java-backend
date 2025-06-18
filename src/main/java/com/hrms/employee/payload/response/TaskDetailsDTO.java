package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailsDTO {
    private Long id;
    private String title;
    private String description;
    private String assignedByUsername;
    private LocalDateTime deadline;
    private String status;
    private Integer priority;
    private String relatedProject; // Nullable
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt; // Nullable
}
