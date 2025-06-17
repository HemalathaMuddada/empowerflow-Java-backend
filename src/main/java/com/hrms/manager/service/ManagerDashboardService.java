package com.hrms.manager.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.DashboardWidgetData;
import com.hrms.employee.service.EmployeeDashboardService;
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible
import com.hrms.lead.payload.response.LeadSpecificWidgetData;
import com.hrms.lead.service.LeadDashboardService;
import com.hrms.manager.payload.response.ManagerDashboardResponseDTO;
import com.hrms.manager.payload.response.ManagerSpecificWidgetData;
import com.hrms.performancemanagement.repository.PerformanceReviewRepository;
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class ManagerDashboardService {
    private static final Logger logger = LoggerFactory.getLogger(ManagerDashboardService.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeDashboardService employeeDashboardService;
    @Autowired
    private LeadDashboardService leadDashboardService; // To get Lead-specific widgets
    @Autowired
    private PerformanceReviewRepository performanceReviewRepository;

    @Transactional(readOnly = true)
    public ManagerDashboardResponseDTO getManagerDashboardData(UserDetailsImpl managerUserDetails) {
        User managerUserEntity = userRepository.findById(managerUserDetails.getId())
            .orElseThrow(() -> new UsernameNotFoundException("Manager user not found: " + managerUserDetails.getUsername()));

        // 1. Get common employee dashboard data for the Manager themselves
        DashboardWidgetData employeeData = employeeDashboardService.getDashboardData(managerUserDetails).getWidgets();

        // 2. Get Lead-specific data (if manager has direct reports, they are also a Lead)
        LeadSpecificWidgetData leadData = null;
        List<User> reportees = userRepository.findByManagerId(managerUserEntity.getId());
        if (reportees != null && !reportees.isEmpty()) {
            // This reuses the LeadDashboardService logic which is good for consistency
            // It will internally fetch reportees again, which is a small overhead but keeps services decoupled
            leadData = leadDashboardService.getLeadDashboardData(managerUserDetails).getLeadSpecificData();
        } else {
            leadData = new LeadSpecificWidgetData(); // Empty default if no reportees
            logger.info("Manager {} has no direct reportees, Lead-specific data will be zero.", managerUserEntity.getUsername());
        }

        // 3. Get Manager-specific widget data
        ManagerSpecificWidgetData managerWidgets = new ManagerSpecificWidgetData();

        // Team Performance Reviews Pending Employee Acknowledgement
        managerWidgets.setTeamPerformanceReviewsPendingAcknowledgement(
            performanceReviewRepository.countByReviewerAndStatusIn(
                managerUserEntity,
                Collections.singletonList("PENDING_EMPLOYEE_ACKNOWLEDGEMENT")
            )
        );

        logger.debug("Manager Dashboard for {}: EmployeeData: {}, LeadData: {}, ManagerSpecificData: {}",
            managerUserEntity.getUsername(), employeeData, leadData, managerWidgets);

        return new ManagerDashboardResponseDTO(employeeData, leadData, managerWidgets);
    }
}
