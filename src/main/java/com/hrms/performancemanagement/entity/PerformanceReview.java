package com.hrms.performancemanagement.entity;

import com.hrms.core.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
//import java.math.BigDecimal; // Not used in this version of entity

@Entity
@Table(name = "performance_reviews", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "review_cycle_id"}, name = "uq_employee_review_cycle")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_cycle_id", nullable = false)
    private ReviewCycle reviewCycle;

    @Lob
    @Column(name = "goals_objectives")
    private String goalsAndObjectives;

    @Lob
    @Column(name = "employee_self_appraisal")
    private String employeeSelfAppraisal;

    @Lob
    @Column(name = "manager_evaluation")
    private String managerEvaluation;

    @Lob
    @Column(name = "employee_comments")
    private String employeeComments;

    @Column(name = "overall_rating_by_manager")
    private Integer overallRatingByManager;

    @Column(name = "final_rating")
    private Integer finalRating;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "submitted_by_employee_at")
    private LocalDateTime submittedByEmployeeAt;

    @Column(name = "reviewed_by_manager_at")
    private LocalDateTime reviewedByManagerAt;

    @Column(name = "acknowledged_by_employee_at")
    private LocalDateTime acknowledgedByEmployeeAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
