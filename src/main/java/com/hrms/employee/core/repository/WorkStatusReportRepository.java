package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.WorkStatusReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkStatusReportRepository extends JpaRepository<WorkStatusReport, Long> {
    // Custom query methods can be added here later if needed
    // List<WorkStatusReport> findByEmployeeAndReportDateBetween(User employee, LocalDate startDate, LocalDate endDate);
}
