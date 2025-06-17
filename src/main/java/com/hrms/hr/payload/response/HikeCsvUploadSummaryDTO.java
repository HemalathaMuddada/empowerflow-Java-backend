package com.hrms.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HikeCsvUploadSummaryDTO {
    private int totalRecordsProcessed = 0;
    private int successfullyImported = 0;
    private int failedRecords = 0;
    private List<String> errorDetails = new ArrayList<>();

    public void incrementTotalRecordsProcessed() {
        this.totalRecordsProcessed++;
    }

    public void incrementSuccessfullyImported() {
        this.successfullyImported++;
    }

    public void incrementFailedRecords() {
        this.failedRecords++;
    }

    public void addErrorDetail(String error) {
        this.errorDetails.add(error);
    }
}
