package com.hrms.hr.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.enums.ConcernStatus;
import com.hrms.employee.core.repository.ConcernRepository;
import com.hrms.hr.payload.response.HRDashboardResponseDTO;
import com.hrms.hr.payload.response.HRDashboardWidgetData;
import com.hrms.performancemanagement.repository.PerformanceReviewRepository;
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Assuming ResourceNotFoundException is accessible if needed, though not directly used here
// import com.hrms.hr.service.ResourceNotFoundException;

@Service
public class HRDashboardService {
    private static final Logger logger = LoggerFactory.getLogger(HRDashboardService.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PerformanceReviewRepository performanceReviewRepository;
    @Autowired
    private ConcernRepository concernRepository;

    @Transactional(readOnly = true)
    public HRDashboardResponseDTO getHRDashboardData(UserDetailsImpl hrUserDetails) {
        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        Long companyIdForScope = (hrUser.getCompany() != null) ? hrUser.getCompany().getId() : null;
        logger.info("Fetching HR dashboard data for HR User: {}, Company Scope ID: {}", hrUser.getUsername(), companyIdForScope);

        HRDashboardWidgetData widgets = new HRDashboardWidgetData();

        // Total Active Employees
        if (companyIdForScope != null) {
            widgets.setTotalActiveEmployeesInScope(userRepository.countByCompanyIdAndIsActiveTrue(companyIdForScope));
        } else { // Global HR
            widgets.setTotalActiveEmployeesInScope(userRepository.countByIsActiveTrue());
        }

        // Performance Reviews Pending HR Action
        String pendingHRReviewStatus = "PENDING_HR_REVIEW"; // Assuming this is the correct status string
        if (companyIdForScope != null) {
            widgets.setPerformanceReviewsPendingHRAction(
                performanceReviewRepository.countByStatusAndEmployee_CompanyId(pendingHRReviewStatus, companyIdForScope)
            );
        } else { // Global HR
            widgets.setPerformanceReviewsPendingHRAction(
                performanceReviewRepository.countByStatus(pendingHRReviewStatus)
            );
        }

        // New Employees Last 30 Days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        if (companyIdForScope != null) {
            widgets.setNewEmployeesLast30DaysInScope(
                userRepository.countByCompanyIdAndCreatedAtAfter(companyIdForScope, thirtyDaysAgo)
            );
        } else { // Global HR
            widgets.setNewEmployeesLast30DaysInScope(
                userRepository.countByCreatedAtAfter(thirtyDaysAgo)
            );
        }

        // Open Concerns
        if (companyIdForScope != null) {
            // Assumes concern's company is based on who raised it.
            widgets.setOpenConcernsInScope(
                concernRepository.countByStatusAndRaisedBy_CompanyId(ConcernStatus.OPEN, companyIdForScope)
            );
        } else { // Global HR
            widgets.setOpenConcernsInScope(
                concernRepository.countByStatus(ConcernStatus.OPEN)
            );
        }

        logger.debug("HR Dashboard Widgets Data: {}", widgets);
        return new HRDashboardResponseDTO(widgets);
    }
}
