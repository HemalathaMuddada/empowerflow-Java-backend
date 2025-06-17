package com.hrms.employee.core.entity;

import com.hrms.core.entity.User;
import com.hrms.employee.core.enums.RegularizationReason;
import com.hrms.employee.core.enums.RegularizationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_regularization_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RegularizationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    // Nullable if the regularization is for a day with no attendance record yet (e.g. forgot to login at all)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private Attendance attendanceRecord;

    @Column(nullable = false)
    private LocalDate requestDate; // The date for which regularization is requested

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegularizationReason reasonType;

    @Column(columnDefinition = "TEXT") // Used if reasonType is OTHER
    private String customReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegularizationStatus status = RegularizationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String approverComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
