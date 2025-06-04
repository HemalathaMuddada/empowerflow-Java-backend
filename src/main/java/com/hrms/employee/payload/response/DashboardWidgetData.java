package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWidgetData {
    private int upcomingHolidaysCount = 0;
    private Map<String, Double> availableLeaveBalance = Collections.emptyMap();
    private int pendingTasksCount = 0;
    private int pendingRegularizationRequestsCount = 0;
}
