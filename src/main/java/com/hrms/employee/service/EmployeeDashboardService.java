package com.hrms.employee.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
// Import other repositories as needed for actual data fetching later
// import com.hrms.employee.core.repository.HolidayRepository;
// import com.hrms.employee.core.repository.LeaveRequestRepository;
// import com.hrms.employee.core.repository.TaskRepository;
// import com.hrms.employee.core.repository.RegularizationRequestRepository;
import com.hrms.employee.payload.response.DashboardWidgetData;
import com.hrms.employee.payload.response.EmployeeDashboardResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class EmployeeDashboardService {

    @Autowired
    private UserRepository userRepository;

    // @Autowired
    // private HolidayRepository holidayRepository;
    // @Autowired
    // private LeaveRequestRepository leaveRequestRepository;
    // @Autowired
    // private TaskRepository taskRepository;
    // @Autowired
    // private RegularizationRequestRepository regularizationRequestRepository;

    @Transactional(readOnly = true)
    public EmployeeDashboardResponse getDashboardData(UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + currentUser.getId()));

        String welcomeMessage = "Welcome, " + user.getFirstName() + "!";

        // Placeholder data for widgets for now
        // Actual data fetching logic will be implemented in subsequent tasks
        DashboardWidgetData widgetData = new DashboardWidgetData(
                0, // upcomingHolidaysCount
                Collections.emptyMap(), // availableLeaveBalance
                0, // pendingTasksCount
                0  // pendingRegularizationRequestsCount
        );

        // Example for fetching actual data (to be uncommented and implemented later)
        // int upcomingHolidays = holidayRepository.countUpcomingHolidays(...);
        // Map<String, Double> leaveBalances = leaveRequestService.getAvailableLeaveBalance(user);
        // int pendingTasks = taskRepository.countByAssignedToAndStatus(user, TaskStatus.TODO);
        // int pendingRegularizations = regularizationRepository.countByEmployeeAndStatus(user, RegularizationStatus.PENDING);
        // widgetData.setUpcomingHolidaysCount(upcomingHolidays);
        // widgetData.setAvailableLeaveBalance(leaveBalances);
        // widgetData.setPendingTasksCount(pendingTasks);
        // widgetData.setPendingRegularizationRequestsCount(pendingRegularizations);


        return new EmployeeDashboardResponse(welcomeMessage, widgetData);
    }
}
