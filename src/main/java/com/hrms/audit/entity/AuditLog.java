package com.hrms.audit.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_log_actor_username", columnList = "actor_username"), // DB column name
    @Index(name = "idx_audit_log_action_type", columnList = "action_type"),     // DB column name
    @Index(name = "idx_audit_log_target_entity", columnList = "target_entity_type, target_entity_id") // DB column names
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "actor_username", length = 100, nullable = false)
    private String actorUsername;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "action_type", length = 100, nullable = false)
    private String actionType;

    @Column(name = "target_entity_type", length = 100)
    private String targetEntityType;

    @Column(name = "target_entity_id", length = 100)
    private String targetEntityId;

    @Lob
    @Column(name = "details")
    private String details;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "status", length = 20, nullable = false)
    private String status;
}
