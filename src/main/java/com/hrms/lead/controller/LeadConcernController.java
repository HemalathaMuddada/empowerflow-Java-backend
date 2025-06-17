package com.hrms.lead.controller;

import com.hrms.employee.payload.request.RaiseConcernRequest; // Reusing
import com.hrms.employee.payload.response.ConcernResponseDTO; // Reusing
import com.hrms.lead.service.LeadConcernService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lead/concerns")
@PreAuthorize("hasRole('ROLE_LEAD')")
public class LeadConcernController {

    @Autowired
    private LeadConcernService leadConcernService;

    @PostMapping("/raise")
    public ResponseEntity<ConcernResponseDTO> raiseNewLeadConcern(
            @Valid @RequestBody RaiseConcernRequest concernRequest,
            @AuthenticationPrincipal UserDetailsImpl leadUser) {
        ConcernResponseDTO response = leadConcernService.raiseLeadConcern(concernRequest, leadUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
