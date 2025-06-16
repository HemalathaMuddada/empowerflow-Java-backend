package com.hrms.manager.controller;

import com.hrms.manager.payload.response.ManagerDashboardResponseDTO;
import com.hrms.manager.service.ManagerDashboardService;
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible
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
@RequestMapping("/api/manager/dashboard")
@PreAuthorize("hasRole('ROLE_MANAGER')")
public class ManagerDashboardController {

    @Autowired
    private ManagerDashboardService managerDashboardService;

    @GetMapping("/")
    public ResponseEntity<ManagerDashboardResponseDTO> getManagerDashboard(
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            ManagerDashboardResponseDTO dashboardData = managerDashboardService.getManagerDashboardData(managerUser);
            return ResponseEntity.ok(dashboardData);
        } catch (ResourceNotFoundException ex) { // If managerUser themselves not found by service
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex server-side for debugging
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching manager dashboard data.", ex);
        }
    }
}
