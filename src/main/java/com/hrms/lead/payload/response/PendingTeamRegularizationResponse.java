package com.hrms.lead.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingTeamRegularizationResponse {
    private List<TeamRegularizationRequestDTO> pendingRequests;
}
