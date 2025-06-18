package com.hrms.lead.controller;

import com.hrms.exception.ResourceNotFoundException;
import com.hrms.lead.payload.response.LeadDashboardResponseDTO;
import com.hrms.lead.service.LeadDashboardService;
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
@RequestMapping("/api/lead/dashboard")
@PreAuthorize("hasRole('ROLE_LEAD')")
public class LeadDashboardController {

    @Autowired
    private LeadDashboardService leadDashboardService;

    @GetMapping("/")
    public ResponseEntity<LeadDashboardResponseDTO> getLeadDashboard(
            @AuthenticationPrincipal UserDetailsImpl leadUser) {
        try {
            LeadDashboardResponseDTO dashboardData = leadDashboardService.getLeadDashboardData(leadUser);
            return ResponseEntity.ok(dashboardData);
        } catch (ResourceNotFoundException ex) { // If leadUser themselves not found by service
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching lead dashboard data.", ex);
        }
    }
}
