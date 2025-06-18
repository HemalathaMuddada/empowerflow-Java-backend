package com.hrms.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteeReassignmentResponse {
    private int countReassigned;
    private String message;
}
