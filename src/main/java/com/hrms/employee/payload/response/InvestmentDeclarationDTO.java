package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentDeclarationDTO {
    private Long id;
    private Long employeeId;
    private String employeeName; // For display
    private String declarationYear;
    private BigDecimal itDeclarationAmount;
    private BigDecimal fbpOptedAmount;
    private String itDocumentUrl; // URL of the uploaded proof
    private String fbpChoicesJson;
    private String status;
    private LocalDateTime submittedAt;
    private String reviewedByName; // Username or full name of HR who reviewed
    private LocalDateTime reviewedAt;
    private String hrComments;
    private LocalDateTime updatedAt;
}
