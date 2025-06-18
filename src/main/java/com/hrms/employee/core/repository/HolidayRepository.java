package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByCompanyId(Long companyId);
    List<Holiday> findByIsGlobalTrue();
    List<Holiday> findByDate(LocalDate date); // Already implicitly used in AttendanceAlertService, good to formalize
    boolean existsByDateAndIsGlobalTrue(LocalDate date);
    boolean existsByCompanyIdAndDate(Long companyId, LocalDate date);
    Optional<Holiday> findByIdAndIsGlobalTrue(Long id);
}
