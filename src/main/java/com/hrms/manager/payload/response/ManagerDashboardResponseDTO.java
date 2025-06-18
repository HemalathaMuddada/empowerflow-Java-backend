package com.hrms.manager.payload.response;

import com.hrms.employee.payload.response.DashboardWidgetData; // Employee's dashboard data
import com.hrms.lead.payload.response.LeadSpecificWidgetData;   // Lead's specific data
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardResponseDTO {
    private DashboardWidgetData employeeDashboardData;
    private LeadSpecificWidgetData leadDashboardData;   // Nullable if manager has no direct reports or not acting as lead
    private ManagerSpecificWidgetData managerSpecificData;
}
