package com.hrms.audit.repository;

import com.hrms.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    // JpaSpecificationExecutor<AuditLog> not JpaSpecificationExecutor<AuditLog, Long>
}
