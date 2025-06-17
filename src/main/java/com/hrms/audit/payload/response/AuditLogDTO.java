package com.hrms.audit.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private LocalDateTime timestamp;
    private String actorUsername;
    private Long actorId;
    private String actionType;
    private String targetEntityType;
    private String targetEntityId;
    private String details;
    private String ipAddress;
    private String status;
}
