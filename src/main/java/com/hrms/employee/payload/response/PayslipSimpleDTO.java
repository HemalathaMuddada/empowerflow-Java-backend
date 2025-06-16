package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayslipSimpleDTO {
    private Long id;
    private LocalDate payPeriodEndDate;
    private BigDecimal netSalary;
    private String fileUrl; // Optional
}
