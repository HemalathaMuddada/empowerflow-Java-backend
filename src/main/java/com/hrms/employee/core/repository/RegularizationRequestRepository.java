package com.hrms.employee.core.repository;

import com.hrms.core.entity.User;
import com.hrms.employee.core.entity.RegularizationRequest;
import com.hrms.employee.core.enums.RegularizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegularizationRequestRepository extends JpaRepository<RegularizationRequest, Long> {
    List<RegularizationRequest> findByEmployeeInAndStatus(List<User> employees, RegularizationStatus status);
    long countByEmployeeInAndStatus(List<User> employees, RegularizationStatus status); // Added for dashboard count
    long countByEmployeeAndStatus(User employee, RegularizationStatus status);
    // List<RegularizationRequest> findByEmployee(User employee);
    // List<RegularizationRequest> findByStatus(RegularizationStatus status);
}
