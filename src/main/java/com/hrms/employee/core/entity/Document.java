package com.hrms.employee.core.entity;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.employee.core.enums.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id") // Nullable if company-wide or global policy
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id") // Nullable if employee personal or global policy
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String displayName; // User-friendly name for the document

    @Column(nullable = false)
    private String fileName; // Actual stored file name (e.g., UUID.pdf)

    @Column(nullable = false)
    private String fileUrl; // Path or URL to access the file

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id") // User who uploaded this document
    private User uploadedBy;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean isCompanyWide = false; // If true, applies to all employees of 'company_id'

    @Column(nullable = false)
    private boolean isGlobalPolicy = false; // If true, applies to all users, company_id might be null

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
