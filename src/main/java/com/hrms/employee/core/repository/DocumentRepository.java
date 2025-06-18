package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Document;
import com.hrms.employee.core.enums.DocumentType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // For filtering by type
    @Query("SELECT d FROM Document d WHERE d.employee.id = :employeeId AND d.isRestrictedToHR = false AND (:documentType IS NULL OR d.documentType = :documentType)")
    List<Document> findByEmployeeIdAndOptionalDocumentTypeAndIsRestrictedToHRFalse(@Param("employeeId") Long employeeId, @Param("documentType") DocumentType documentType);

    @Query("SELECT d FROM Document d WHERE d.company.id = :companyId AND d.isCompanyWide = true AND d.isRestrictedToHR = false AND (:documentType IS NULL OR d.documentType = :documentType)")
    List<Document> findByCompanyIdAndIsCompanyWideTrueAndOptionalDocumentTypeAndIsRestrictedToHRFalse(@Param("companyId") Long companyId, @Param("documentType") DocumentType documentType);

    @Query("SELECT d FROM Document d WHERE d.isGlobalPolicy = true AND d.isRestrictedToHR = false AND (:documentType IS NULL OR d.documentType = :documentType)")
    List<Document> findByIsGlobalPolicyTrueAndOptionalDocumentTypeAndIsRestrictedToHRFalse(@Param("documentType") DocumentType documentType);

    // Queries for HR access (includes restricted documents)
    @Query("SELECT d FROM Document d WHERE d.employee.id = :employeeId AND (:documentType IS NULL OR d.documentType = :documentType)")
    List<Document> findAllByEmployeeIdAndOptionalDocumentType(@Param("employeeId") Long employeeId, @Param("documentType") DocumentType documentType);

    @Query("SELECT d FROM Document d WHERE d.company.id = :companyId AND d.isCompanyWide = true AND (:documentType IS NULL OR d.documentType = :documentType)")
    List<Document> findAllByCompanyIdAndIsCompanyWideTrueAndOptionalDocumentType(@Param("companyId") Long companyId, @Param("documentType") DocumentType documentType);

    @Query("SELECT d FROM Document d WHERE d.isGlobalPolicy = true AND (:documentType IS NULL OR d.documentType = :documentType)")
    List<Document> findAllByIsGlobalPolicyTrueAndOptionalDocumentType(@Param("documentType") DocumentType documentType);

    Page<Document> findByIsGlobalPolicyTrue(Pageable pageable); // Added for Super Admin global policy listing

    // Simpler versions if type filter is not needed (can be handled by passing null to above)
    // List<Document> findByEmployeeId(Long employeeId);
    // List<Document> findByCompanyIdAndIsCompanyWideTrue(Long companyId);
    // List<Document> findByIsGlobalPolicyTrue(); // Non-paginated version

}
