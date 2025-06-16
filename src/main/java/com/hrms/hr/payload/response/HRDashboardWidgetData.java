package com.hrms.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRDashboardWidgetData {
    private long totalActiveEmployeesInScope = 0;
    private long performanceReviewsPendingHRAction = 0;
    private long newEmployeesLast30DaysInScope = 0;
    private long openConcernsInScope = 0;
    // Note: "InScope" means either HR's company or global if HR is global.
}
