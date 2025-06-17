package com.hrms.lead.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberHikeDTO {
    private Long employeeId;
    private String employeeName;
    private Long hikeId;
    private BigDecimal hikePercentage;
    private BigDecimal hikeAmount;
    private BigDecimal oldSalary;
    private BigDecimal newSalary;
    private LocalDate effectiveDate;
    private String promotionTitle; // Nullable
    private LocalDateTime processedAt;
    // Adding comments from Hike entity might be useful too
    private String comments; // Nullable
}
