package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    // Custom query methods can be added here later if needed
    // List<Document> findByEmployee(User employee);
    // List<Document> findByCompanyAndIsCompanyWide(Company company, boolean isCompanyWide);
    // List<Document> findByIsGlobalPolicy(boolean isGlobalPolicy);
}
