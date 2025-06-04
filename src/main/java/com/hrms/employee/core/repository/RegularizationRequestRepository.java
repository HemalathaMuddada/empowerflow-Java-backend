package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.RegularizationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegularizationRequestRepository extends JpaRepository<RegularizationRequest, Long> {
    // Custom query methods can be added here later if needed
    // List<RegularizationRequest> findByEmployee(User employee);
    // List<RegularizationRequest> findByStatus(RegularizationStatus status);
}
