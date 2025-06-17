package com.hrms.hr.controller;

import com.hrms.hr.payload.response.HRDashboardResponseDTO;
import com.hrms.hr.service.HRDashboardService;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/hr/dashboard")
@PreAuthorize("hasAnyRole('ROLE_HR', 'ROLE_MANAGER')") // Managers might also have a dashboard view, HR is primary
public class HRDashboardController {

    @Autowired
    private HRDashboardService hrDashboardService;

    @GetMapping("/")
    public ResponseEntity<HRDashboardResponseDTO> getHRDashboard(
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            HRDashboardResponseDTO dashboardData = hrDashboardService.getHRDashboardData(hrUser);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception ex) {
            // Log ex server-side
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching HR dashboard data.", ex);
        }
    }
}
