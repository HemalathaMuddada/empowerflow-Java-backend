package com.hrms.lead.controller;

import com.hrms.employee.payload.request.WorkStatusReportRequest; // Reusing
import com.hrms.employee.payload.response.WorkStatusReportResponse; // Reusing
import com.hrms.lead.service.LeadWorkStatusReportService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lead/reports/status")
@PreAuthorize("hasRole('ROLE_LEAD')")
public class LeadWorkStatusReportController {

    @Autowired
    private LeadWorkStatusReportService leadWorkStatusReportService;

    @PostMapping("/submit")
    public ResponseEntity<WorkStatusReportResponse> submitLeadDailyReport(
            @Valid @RequestBody WorkStatusReportRequest reportRequest,
            @AuthenticationPrincipal UserDetailsImpl leadUser) {
        WorkStatusReportResponse response = leadWorkStatusReportService.submitLeadReport(reportRequest, leadUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
