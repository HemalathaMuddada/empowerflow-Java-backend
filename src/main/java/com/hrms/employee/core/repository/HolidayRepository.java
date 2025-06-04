package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    // Custom query methods can be added here later if needed
    // List<Holiday> findByCompanyOrIsGlobal(Company company, boolean isGlobal);
    // List<Holiday> findByDateBetweenAndCompany(LocalDate startDate, LocalDate endDate, Company company);
    // List<Holiday> findByDateBetweenAndIsGlobal(LocalDate startDate, LocalDate endDate, boolean isGlobal);
}
