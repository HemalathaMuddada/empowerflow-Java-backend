package com.hrms.lead.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveActionRequest {

    @NotBlank(message = "Action cannot be blank")
    @Pattern(regexp = "APPROVE|REJECT", message = "Action must be either APPROVE or REJECT")
    private String action; // "APPROVE" or "REJECT"

    private String managerComment; // Nullable
}
