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
    List<LeaveRequest> findByEmployeeInAndStatus(List<User> employees, LeaveStatus status);
    long countByEmployeeInAndStatus(List<User> employees, LeaveStatus status); // Added for dashboard count

    @Query("SELECT COUNT(lr) > 0 FROM LeaveRequest lr WHERE lr.employee = :employee AND lr.status = com.hrms.employee.core.enums.LeaveStatus.APPROVED AND :checkDate BETWEEN lr.startDate AND lr.endDate")
    boolean hasApprovedLeaveForDate(@Param("employee") User employee, @Param("checkDate") LocalDate checkDate);
}
