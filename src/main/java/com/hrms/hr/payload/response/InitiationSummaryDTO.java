package com.hrms.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiationSummaryDTO {
    private int totalRequested;
    private int successfullyInitiatedCount = 0;
    private int alreadyExistsSkippedCount = 0;
    private int employeeNotFoundSkippedCount = 0;
    private int managerMissingSkippedCount = 0;
    private int permissionDeniedSkippedCount = 0;
    private List<String> details = new ArrayList<>(); // For individual error or success messages

    public InitiationSummaryDTO(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public void incrementSuccessfullyInitiated() {
        this.successfullyInitiatedCount++;
    }
    public void incrementAlreadyExistsSkipped() {
        this.alreadyExistsSkippedCount++;
    }
    public void incrementEmployeeNotFoundSkipped() {
        this.employeeNotFoundSkippedCount++;
    }
    public void incrementManagerMissingSkipped() {
        this.managerMissingSkippedCount++;
    }
    public void incrementPermissionDeniedSkipped() {
        this.permissionDeniedSkippedCount++;
    }
    public void addDetail(String detailMessage) {
        this.details.add(detailMessage);
    }
}
