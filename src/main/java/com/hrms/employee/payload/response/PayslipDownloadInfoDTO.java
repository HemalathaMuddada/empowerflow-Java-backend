package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayslipDownloadInfoDTO {
    private Long payslipId;
    private String fileName;
    private String fileUrlMock; // e.g., "/mock-files/payslip-{id}.pdf"
    private String message;
}
