package com.hrms.lead.payload.response;

import com.hrms.employee.payload.response.DashboardWidgetData; // Employee's dashboard data
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadDashboardResponseDTO {
    private DashboardWidgetData employeeDashboardData; // Common data for the Lead as an employee
    private LeadSpecificWidgetData leadSpecificData;   // Data specific to their role as a Lead
}
