package com.hrms.lead.controller;

import com.hrms.lead.payload.response.TeamHikeSummaryResponse;
import com.hrms.lead.service.LeadHikeService;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lead/hikes")
@PreAuthorize("hasRole('ROLE_LEAD')")
public class LeadHikeController {

    @Autowired
    private LeadHikeService leadHikeService;

    @GetMapping("/team-summary")
    public ResponseEntity<TeamHikeSummaryResponse> getTeamHikeDetails(
            @AuthenticationPrincipal UserDetailsImpl leadUser) {
        TeamHikeSummaryResponse response = leadHikeService.getTeamHikeInformation(leadUser);
        return ResponseEntity.ok(response);
    }
}
