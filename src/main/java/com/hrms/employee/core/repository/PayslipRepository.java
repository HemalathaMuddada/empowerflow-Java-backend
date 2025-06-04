package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    // Custom query methods can be added here later if needed
}
