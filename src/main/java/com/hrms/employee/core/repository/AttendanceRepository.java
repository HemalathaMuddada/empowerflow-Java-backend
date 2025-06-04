package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    // Custom query methods can be added here later if needed
    // Optional<Attendance> findByEmployeeAndWorkDate(User employee, LocalDate workDate);
}
