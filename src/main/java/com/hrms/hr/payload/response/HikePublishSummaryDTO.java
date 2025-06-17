package com.hrms.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HikePublishSummaryDTO {
    private int totalRequested;
    private int successfullyPublishedCount = 0;
    private int alreadyPublishedSkippedCount = 0;
    private int permissionDeniedSkippedCount = 0;
    private int notFoundSkippedCount = 0;
    private int emailSentCount = 0;
    private int emailFailedCount = 0;
    private List<String> details = new ArrayList<>(); // To store messages like "Hike ID 123: Published and email sent." or "Hike ID 456: Error sending email."

    public HikePublishSummaryDTO(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public void incrementSuccessfullyPublished() {
        this.successfullyPublishedCount++;
    }
    public void incrementAlreadyPublishedSkipped() {
        this.alreadyPublishedSkippedCount++;
    }
    public void incrementPermissionDeniedSkipped() {
        this.permissionDeniedSkippedCount++;
    }
    public void incrementNotFoundSkipped() {
        this.notFoundSkippedCount++;
    }
    public void incrementEmailSent() {
        this.emailSentCount++;
    }
    public void incrementEmailFailed() {
        this.emailFailedCount++;
    }
    public void addDetail(String detail) {
        this.details.add(detail);
    }
}
