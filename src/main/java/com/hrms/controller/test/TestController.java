package com.hrms.controller.test;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/secure-ping")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> securePing() {
        return ResponseEntity.ok("Pong! Authenticated access successful.");
    }

    @GetMapping("/admin-ping")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<String> adminPing() {
        return ResponseEntity.ok("Pong! Super Admin access successful.");
    }

    @GetMapping("/employee-ping")
    @PreAuthorize("hasRole('ROLE_EMPLOYEE')")
    public ResponseEntity<String> employeePing() {
        return ResponseEntity.ok("Pong! Employee access successful.");
    }

    // Example of a more specific role or combination of roles
    @GetMapping("/hr-ping")
    @PreAuthorize("hasRole('ROLE_HR')")
    public ResponseEntity<String> hrPing() {
        return ResponseEntity.ok("Pong! HR access successful.");
    }

    @GetMapping("/manager-ping")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<String> managerPing() {
        return ResponseEntity.ok("Pong! Manager access successful.");
    }

     @GetMapping("/lead-ping")
    @PreAuthorize("hasRole('ROLE_LEAD')")
    public ResponseEntity<String> leadPing() {
        return ResponseEntity.ok("Pong! Lead access successful.");
    }
}
