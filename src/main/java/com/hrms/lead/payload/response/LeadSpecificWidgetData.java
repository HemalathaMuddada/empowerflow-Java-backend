package com.hrms.lead.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadSpecificWidgetData {
    private long teamPendingLeaveApprovalsCount = 0;
    private long teamPendingRegularizationApprovalsCount = 0;
    private long tasksAssignedByMeOverdueCount = 0;
}
