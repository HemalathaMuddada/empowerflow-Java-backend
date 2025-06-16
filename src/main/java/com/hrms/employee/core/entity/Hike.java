package com.hrms.employee.core.entity;

import com.hrms.core.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_hikes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Hike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(precision = 5, scale = 2) // e.g., 10.00 for 10%
    private BigDecimal hikePercentage;

    @Column(precision = 10, scale = 2)
    private BigDecimal hikeAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal oldSalary;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal newSalary;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    private String promotionTitle; // New title if promoted

    private String hikeLetterDocumentUrl; // Link to the hike letter (Document entity might be better)

    @Column(columnDefinition = "TEXT")
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id", nullable = false)
    private User processedBy; // HR or Manager who processed the hike

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt; // Nullable
}
