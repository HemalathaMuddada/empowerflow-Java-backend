package com.hrms.manager.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerSpecificWidgetData {
    private long teamPerformanceReviewsPendingAcknowledgement = 0;
    // Add other manager-specific counts here if any in future,
    // e.g., count of direct reportees whose performance goals are not set, etc.
}
