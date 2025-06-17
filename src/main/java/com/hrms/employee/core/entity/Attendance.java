package com.hrms.employee.core.entity;

import com.hrms.core.entity.User;
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
@Table(name = "employee_attendance", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "workDate"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    private LocalDateTime logoutTime;

    @Column(nullable = false)
    private LocalDate workDate;

    private Double totalHours;

    @Column(nullable = false)
    private boolean isRegularized = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "underwork_alert_sent_at")
    private LocalDateTime underworkAlertSentAt;

    @Column(name = "missed_logout_alert_sent_at")
    private LocalDateTime missedLogoutAlertSentAt;

    @Column(name = "early_logout_alert_sent_at")
    private LocalDateTime earlyLogoutAlertSentAt;

    @Column(name = "late_login_alert_sent_at")
    private LocalDateTime lateLoginAlertSentAt;
}
