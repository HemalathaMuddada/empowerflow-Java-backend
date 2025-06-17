package com.hrms.employee.core.entity;

import com.hrms.core.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_investment_declarations", indexes = {
    @Index(name = "idx_inv_decl_employee_year_search", columnList = "employee_id, declaration_year")
    // Renamed from idx_inv_decl_employee_year to avoid conflict with potential unique constraint name by some DBs
    // The unique constraint itself also creates an index typically.
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_employee_declaration_year", columnNames = {"employee_id", "declaration_year"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class InvestmentDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(name = "declaration_year", length = 10, nullable = false)
    private String declarationYear;

    @Column(name = "it_declaration_amount", precision = 19, scale = 2)
    private BigDecimal itDeclarationAmount;

    @Column(name = "fbp_opted_amount", precision = 19, scale = 2)
    private BigDecimal fbpOptedAmount;

    @Column(name = "it_document_url", length = 512)
    private String itDocumentUrl;

    @Lob
    @Column(name = "fbp_choices_json")
    private String fbpChoicesJson;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Lob
    @Column(name = "hr_comments")
    private String hrComments;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
