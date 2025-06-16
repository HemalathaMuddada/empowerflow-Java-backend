package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HikeDetailDTO {
    private Long id;
    private BigDecimal hikePercentage;
    private BigDecimal hikeAmount;
    private BigDecimal oldSalary;
    private BigDecimal newSalary;
    private LocalDate effectiveDate;
    private String promotionTitle; // Nullable
    private String comments;       // Nullable
    private LocalDateTime processedAt;
}
