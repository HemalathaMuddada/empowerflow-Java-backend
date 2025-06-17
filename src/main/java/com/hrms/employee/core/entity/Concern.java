package com.hrms.employee.core.entity;

import com.hrms.core.entity.User;
import com.hrms.employee.core.enums.ConcernStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_concerns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Concern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by_id", nullable = false)
    private User raisedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_against_employee_id")
    private User raisedAgainstEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_against_lead_id")
    private User raisedAgainstLead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_against_manager_id")
    private User raisedAgainstManager;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String concernText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConcernStatus status = ConcernStatus.OPEN;

    private String category; // e.g., "Workload", "Interpersonal", "System Issue"

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;
}
