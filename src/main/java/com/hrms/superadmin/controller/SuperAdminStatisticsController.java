package com.hrms.superadmin.controller;

import com.hrms.security.service.UserDetailsImpl;
import com.hrms.superadmin.payload.response.SystemStatisticsDTO;
import com.hrms.superadmin.service.SuperAdminStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/super-admin/statistics")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminStatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminStatisticsController.class);

    @Autowired
    private SuperAdminStatisticsService superAdminStatisticsService;

    @GetMapping("/system")
    public ResponseEntity<SystemStatisticsDTO> getSystemWideStatistics(
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            logger.info("Received request for system-wide statistics from SuperAdmin: {}", superAdminUser.getUsername());
            SystemStatisticsDTO statistics = superAdminStatisticsService.getSystemStatistics(superAdminUser);
            return ResponseEntity.ok(statistics);
        } catch (Exception ex) {
            logger.error("Error fetching system-wide statistics for SuperAdmin: {}", superAdminUser.getUsername(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching system statistics.", ex);
        }
    }
}
