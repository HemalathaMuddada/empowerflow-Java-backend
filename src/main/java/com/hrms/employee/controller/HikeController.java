package com.hrms.employee.controller;

import com.hrms.employee.payload.response.HikeHistoryResponse;
import com.hrms.employee.service.HikeService;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee/hikes")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class HikeController {

    @Autowired
    private HikeService hikeService;

    @GetMapping("/my-history")
    public ResponseEntity<HikeHistoryResponse> getMyHikeInformation(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        HikeHistoryResponse response = hikeService.getMyHikeHistory(currentUser);
        return ResponseEntity.ok(response);
    }
}
