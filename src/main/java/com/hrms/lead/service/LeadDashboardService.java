package com.hrms.lead.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.enums.LeaveStatus;
import com.hrms.employee.core.enums.RegularizationStatus;
import com.hrms.employee.core.enums.TaskStatus;
import com.hrms.employee.core.repository.LeaveRequestRepository;
import com.hrms.employee.core.repository.RegularizationRequestRepository;
import com.hrms.employee.core.repository.TaskRepository;
import com.hrms.employee.payload.response.DashboardWidgetData;
import com.hrms.employee.service.EmployeeDashboardService;
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible
import com.hrms.lead.payload.response.LeadDashboardResponseDTO;
import com.hrms.lead.payload.response.LeadSpecificWidgetData;
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class LeadDashboardService {
    private static final Logger logger = LoggerFactory.getLogger(LeadDashboardService.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeDashboardService employeeDashboardService; // For common employee widgets
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    @Autowired
    private RegularizationRequestRepository regularizationRequestRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public LeadDashboardResponseDTO getLeadDashboardData(UserDetailsImpl leadUserDetails) {
        User leadUser = userRepository.findById(leadUserDetails.getId())
            .orElseThrow(() -> new UsernameNotFoundException("Lead user not found: " + leadUserDetails.getUsername()));

        // 1. Get common employee dashboard data for the Lead themselves
        DashboardWidgetData employeeData = employeeDashboardService.getDashboardData(leadUserDetails).getWidgets();

        // 2. Get Lead-specific widget data
        LeadSpecificWidgetData leadSpecificData = new LeadSpecificWidgetData();
        List<User> reportees = userRepository.findByManagerId(leadUser.getId());

        if (reportees != null && !reportees.isEmpty()) {
            leadSpecificData.setTeamPendingLeaveApprovalsCount(
                leaveRequestRepository.countByEmployeeInAndStatus(reportees, LeaveStatus.PENDING)
            );
            leadSpecificData.setTeamPendingRegularizationApprovalsCount(
                regularizationRequestRepository.countByEmployeeInAndStatus(reportees, RegularizationStatus.PENDING)
            );
        } else {
            logger.info("Lead {} has no direct reportees.", leadUser.getUsername());
            // Counts will remain 0 as initialized
        }

        List<TaskStatus> openTaskStatuses = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);
        leadSpecificData.setTasksAssignedByMeOverdueCount(
            taskRepository.countByAssignedByAndStatusInAndDeadlineBeforeAndAutoClosedAtIsNull(
                leadUser, openTaskStatuses, LocalDateTime.now()
            )
        );

        return new LeadDashboardResponseDTO(employeeData, leadSpecificData);
    }
}
