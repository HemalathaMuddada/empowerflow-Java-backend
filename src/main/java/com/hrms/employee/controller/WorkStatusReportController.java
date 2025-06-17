package com.hrms.employee.controller;

import com.hrms.employee.payload.request.WorkStatusReportRequest;
import com.hrms.employee.payload.response.WorkStatusReportResponse;
import com.hrms.employee.service.WorkStatusReportService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/reports/status")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class WorkStatusReportController {

    @Autowired
    private WorkStatusReportService workStatusReportService;

    @PostMapping("/submit")
    public ResponseEntity<WorkStatusReportResponse> submitDailyReport(
            @Valid @RequestBody WorkStatusReportRequest reportRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        WorkStatusReportResponse response = workStatusReportService.submitReport(reportRequest, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
