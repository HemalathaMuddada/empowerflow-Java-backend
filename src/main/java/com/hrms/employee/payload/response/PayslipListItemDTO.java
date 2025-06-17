package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayslipListItemDTO {
    private Long id;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private BigDecimal netSalary;
    private LocalDate generatedDate;
    private String status; // e.g., "Generated", "Published", "Available"
}
