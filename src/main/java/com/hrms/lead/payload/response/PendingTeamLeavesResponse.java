package com.hrms.lead.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingTeamLeavesResponse {
    private List<TeamLeaveRequestDTO> pendingRequests;
}
