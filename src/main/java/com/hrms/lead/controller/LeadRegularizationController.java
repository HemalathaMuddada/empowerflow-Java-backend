package com.hrms.lead.controller;

import com.hrms.lead.payload.request.RegularizationActionRequest;
import com.hrms.lead.payload.response.PendingTeamRegularizationResponse;
import com.hrms.lead.payload.response.TeamRegularizationRequestDTO;
import com.hrms.lead.service.LeadRegularizationService;
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
@RequestMapping("/api/lead/regularizations")
@PreAuthorize("hasRole('ROLE_LEAD')")
public class LeadRegularizationController {

    @Autowired
    private LeadRegularizationService leadRegularizationService;

    @GetMapping("/pending-approval")
    public ResponseEntity<PendingTeamRegularizationResponse> getPendingTeamRegularizations(
            @AuthenticationPrincipal UserDetailsImpl leadUser) {
        PendingTeamRegularizationResponse response = leadRegularizationService.getPendingRegularizationRequestsForMyTeam(leadUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{regularizationRequestId}/action")
    public ResponseEntity<TeamRegularizationRequestDTO> takeRegularizationAction(
            @PathVariable Long regularizationRequestId,
            @Valid @RequestBody RegularizationActionRequest actionRequest,
            @AuthenticationPrincipal UserDetailsImpl leadUser) {
        try {
            TeamRegularizationRequestDTO updatedRequest = leadRegularizationService.approveOrRejectRegularization(
                    regularizationRequestId, actionRequest, leadUser);
            return ResponseEntity.ok(updatedRequest);
        } catch (RuntimeException ex) {
            if (ex instanceof AccessDeniedException) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
            } else if (ex instanceof IllegalStateException) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            } else if (ex.getMessage().contains("not found")) { // Basic check for resource not found
                 throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing regularization action", ex);
        }
    }
}
