package com.hrms.employee.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Payslip;
import com.hrms.employee.core.enums.RegularizationStatus;
import com.hrms.employee.core.enums.TaskStatus;
import com.hrms.employee.core.repository.HolidayRepository;
import com.hrms.employee.core.repository.PayslipRepository;
import com.hrms.employee.core.repository.RegularizationRequestRepository;
import com.hrms.employee.core.repository.TaskRepository;
import com.hrms.employee.payload.response.*;
import com.hrms.performancemanagement.entity.PerformanceReview;
import com.hrms.performancemanagement.repository.PerformanceReviewRepository;
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeDashboardService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeDashboardService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private HolidayService holidayService; // Reusing for consistent holiday logic
    @Autowired private LeaveService leaveService;     // Reusing for consistent leave balance logic
    @Autowired private TaskRepository taskRepository;
    @Autowired private RegularizationRequestRepository regularizationRequestRepository;
    @Autowired private PerformanceReviewRepository performanceReviewRepository;
    @Autowired private PayslipRepository payslipRepository;


    @Transactional(readOnly = true)
    public EmployeeDashboardResponse getDashboardData(UserDetailsImpl currentUserDetails) {
        User currentUser = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + currentUserDetails.getId()));

        String welcomeMessage = "Welcome, " + currentUser.getFirstName() + "!";

        // 1. Upcoming Holidays
        // Get all applicable holidays (company + global)
        HolidayListResponse holidayListResponse = holidayService.getApplicableHolidays(currentUserDetails);
        long upcomingHolidaysCount = holidayListResponse.getHolidays().stream()
            .filter(h -> !h.getDate().isBefore(LocalDate.now())) // Today or in future
            .limit(5) // Example: count next 5 upcoming, or just all future ones
            .count();

        // 2. Leave Balances
        EmployeeLeaveSummaryResponse leaveSummary = leaveService.getLeaveBalances(currentUserDetails);
        Map<String, Double> leaveBalancesMap = leaveSummary.getBalances().stream()
            .collect(Collectors.toMap(LeaveBalanceDTO::getLeaveType, LeaveBalanceDTO::getBalance));

        // 3. Pending Tasks
        long pendingTasks = taskRepository.countByAssignedToAndStatusInAndAutoClosedAtIsNull(
            currentUser, List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS));

        // 4. My Pending Regularizations
        long pendingRegs = regularizationRequestRepository.countByEmployeeAndStatus(
            currentUser, RegularizationStatus.PENDING);

        // 5. Active Performance Review
        String activePerformanceReviewStatus = null;
        Long activePerformanceReviewId = null;
        // Define statuses that are considered "not active" or "closed" for a review
        List<String> nonActiveReviewStatuses = List.of("COMPLETED", "CLOSED", "ARCHIVED", "CANCELLED"); // Add more as needed
        Optional<PerformanceReview> activeReviewOpt = performanceReviewRepository
            .findTopByEmployeeAndStatusNotInOrderByCreatedAtDesc(currentUser, nonActiveReviewStatuses);

        if (activeReviewOpt.isPresent()) {
            activePerformanceReviewStatus = activeReviewOpt.get().getStatus();
            activePerformanceReviewId = activeReviewOpt.get().getId();
        }

        // 6. Recent Payslips
        List<Payslip> recentPayslipEntities = payslipRepository.findTop3ByEmployeeOrderByPayPeriodEndDesc(currentUser);
        List<PayslipSimpleDTO> recentPayslipsDto = recentPayslipEntities.stream()
            .map(p -> new PayslipSimpleDTO(p.getId(), p.getPayPeriodEnd(), p.getNetSalary(), p.getFileUrl()))
            .collect(Collectors.toList());


        DashboardWidgetData widgetData = new DashboardWidgetData(
                (int) upcomingHolidaysCount,
                leaveBalancesMap,
                pendingTasks,
                pendingRegs,
                activePerformanceReviewStatus,
                activePerformanceReviewId,
                recentPayslipsDto
        );

        return new EmployeeDashboardResponse(welcomeMessage, widgetData);
    }
}
