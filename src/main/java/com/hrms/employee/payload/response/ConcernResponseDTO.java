package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcernResponseDTO {
    private Long id;
    private Long raisedByEmployeeId;
    private String concernText;
    private String category; // This might include the targetRole hint
    private String status;
    // private String targetRoleHint; // Can be added if Concern entity has a dedicated field
    private LocalDateTime createdAt;
}
