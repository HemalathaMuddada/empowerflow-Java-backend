package com.hrms.hr.entity;

import com.hrms.core.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL; // For @URL if used at entity level, DTO is common
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.time.LocalDateTime;

@Entity
@Table(name = "hiring_resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class) // For @CreatedDate
public class HiringResume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String candidateName;

    @Column(nullable = false, length = 2048) // Assuming URL can be long
    private String resumeLink; // Should be validated as URL at DTO level

    @Column(columnDefinition = "TEXT") // Comma-separated or JSON string
    private String skills;

    @Column(nullable = false, length = 255)
    private String category; // e.g., "Software Engineer L3"

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @CreatedDate // Handled by AuditingEntityListener
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
