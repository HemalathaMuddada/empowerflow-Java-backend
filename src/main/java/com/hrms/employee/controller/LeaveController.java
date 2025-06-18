package com.hrms.employee.controller;

import com.hrms.employee.payload.request.LeaveApplicationRequest;
import com.hrms.employee.payload.response.EmployeeLeaveSummaryResponse;
import com.hrms.employee.payload.response.LeaveHistoryResponse;
import com.hrms.employee.payload.response.LeaveRequestDetailsDTO;
import com.hrms.employee.service.LeaveService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/leaves")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/balances")
    public ResponseEntity<EmployeeLeaveSummaryResponse> getMyLeaveBalances(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        EmployeeLeaveSummaryResponse summaryResponse = leaveService.getLeaveBalances(currentUser);
        return ResponseEntity.ok(summaryResponse);
    }

    @PostMapping("/apply")
    public ResponseEntity<LeaveRequestDetailsDTO> applyForLeave(
            @Valid @RequestBody LeaveApplicationRequest leaveRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        LeaveRequestDetailsDTO createdLeaveRequest = leaveService.applyForLeave(leaveRequest, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLeaveRequest);
    }

    @GetMapping("/history")
    public ResponseEntity<LeaveHistoryResponse> getMyLeaveHistory(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        LeaveHistoryResponse historyResponse = leaveService.getLeaveHistory(currentUser);
        return ResponseEntity.ok(historyResponse);
    }
}
