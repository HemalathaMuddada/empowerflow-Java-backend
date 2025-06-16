package com.hrms.employee.controller;

import com.hrms.employee.payload.response.BirthdayGreetingResponse;
import com.hrms.employee.service.EmployeeWellbeingService;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee/wellbeing")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class EmployeeWellbeingController {

    @Autowired
    private EmployeeWellbeingService employeeWellbeingService;

    @GetMapping("/birthday-greeting")
    public ResponseEntity<BirthdayGreetingResponse> checkBirthdayGreeting(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        BirthdayGreetingResponse response = employeeWellbeingService.getBirthdayGreetingInfo(currentUser);
        return ResponseEntity.ok(response);
    }
}
