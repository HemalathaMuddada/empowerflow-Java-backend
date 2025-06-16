package com.hrms.lead.controller;

import com.hrms.employee.payload.response.LeaveRequestDetailsDTO;
import com.hrms.lead.payload.request.LeaveActionRequest;
import com.hrms.lead.payload.response.PendingTeamLeavesResponse;
import com.hrms.lead.service.LeadLeaveService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/lead/leaves")
@PreAuthorize("hasRole('ROLE_LEAD')")
public class LeadLeaveController {

    @Autowired
    private LeadLeaveService leadLeaveService;

    @GetMapping("/pending-approval")
    public ResponseEntity<PendingTeamLeavesResponse> getPendingTeamLeaves(
            @AuthenticationPrincipal UserDetailsImpl leadUser) {
        PendingTeamLeavesResponse response = leadLeaveService.getPendingLeaveRequestsForMyTeam(leadUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{leaveRequestId}/action")
    public ResponseEntity<LeaveRequestDetailsDTO> takeLeaveAction(
            @PathVariable Long leaveRequestId,
            @Valid @RequestBody LeaveActionRequest actionRequest,
            @AuthenticationPrincipal UserDetailsImpl leadUser) {
        try {
            LeaveRequestDetailsDTO updatedLeaveRequest = leadLeaveService.approveOrRejectLeave(leaveRequestId, actionRequest, leadUser);
            return ResponseEntity.ok(updatedLeaveRequest);
        } catch (RuntimeException ex) { // Catch specific exceptions like ResourceNotFound, IllegalState
            if (ex instanceof AccessDeniedException) {
                 throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
            } else if (ex instanceof IllegalStateException) {
                 throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
            // Default to internal server error or more specific handling
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing leave action", ex);
        }
    }
}
