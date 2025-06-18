package com.hrms.superadmin.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatisticsDTO {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long totalCompanies;
    private long activeCompanies;
    private long inactiveCompanies;
    private long totalSystemConfigurations;
    private long totalAuditLogEntries;
    private long performanceReviewCyclesCount;
    private long activePerformanceReviewsCount; // Reviews not in a final state like COMPLETED/CLOSED
}
