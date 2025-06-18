package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWidgetData {
    private int upcomingHolidaysCount = 0;
    private Map<String, Double> leaveBalances = Collections.emptyMap(); // Renamed for clarity
    private long pendingTasksCount = 0; // Changed to long
    private long myPendingRegularizationsCount = 0; // Changed to long

    // New fields for performance review and payslips
    private String activePerformanceReviewStatus; // Nullable
    private Long activePerformanceReviewId;     // Nullable
    private List<PayslipSimpleDTO> recentPayslips = Collections.emptyList();
}
