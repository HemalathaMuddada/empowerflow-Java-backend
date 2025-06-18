package com.hrms.employee.controller;

import com.hrms.employee.payload.response.EmployeeDashboardResponse;
import com.hrms.employee.payload.response.EmployeeProfileResponse;
import com.hrms.employee.service.EmployeeDashboardService;
import com.hrms.employee.service.EmployeeProfileService;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')") // Allow other employee-like roles too
public class EmployeePortalController {

    @Autowired
    private EmployeeProfileService employeeProfileService;

    @Autowired
    private EmployeeDashboardService employeeDashboardService;

    @GetMapping("/profile")
    public ResponseEntity<EmployeeProfileResponse> getEmployeeProfile(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        EmployeeProfileResponse profileResponse = employeeProfileService.getCurrentEmployeeProfile(currentUser);
        return ResponseEntity.ok(profileResponse);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        EmployeeDashboardResponse dashboardResponse = employeeDashboardService.getDashboardData(currentUser);
        return ResponseEntity.ok(dashboardResponse);
    }
}
