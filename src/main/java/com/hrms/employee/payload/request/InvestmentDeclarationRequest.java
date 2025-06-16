package com.hrms.employee.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentDeclarationRequest {

    @NotBlank(message = "Declaration year cannot be blank.")
    @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Declaration year must be in YYYY-YYYY format (e.g., 2023-2024).")
    private String declarationYear;

    @PositiveOrZero(message = "IT declaration amount must be positive or zero if provided.")
    private BigDecimal itDeclarationAmount; // Optional

    @PositiveOrZero(message = "FBP opted amount must be positive or zero if provided.")
    private BigDecimal fbpOptedAmount;      // Optional

    private String fbpChoicesJson;          // Optional, TEXT for JSON details
}
