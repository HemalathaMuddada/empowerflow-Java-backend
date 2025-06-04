package com.hrms.employee.core.repository;

import com.hrms.core.entity.User;
import com.hrms.employee.core.entity.LeaveRequest;
import com.hrms.employee.core.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeAndStatus(User employee, LeaveStatus status);
    List<LeaveRequest> findByEmployeeOrderByCreatedAtDesc(User employee);
}
