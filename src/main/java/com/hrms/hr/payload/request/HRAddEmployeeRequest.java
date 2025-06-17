package com.hrms.hr.payload.request;

import io.swagger.v3.oas.annotations.media.Schema; // Added
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for HR to add a new employee.")
public class HRAddEmployeeRequest {

    @NotBlank(message = "First name cannot be blank")
    @Schema(description = "Employee's first name.", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Schema(description = "Employee's last name.", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Desired username for the employee.", example = "newemp01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must be less than 100 characters")
    @Schema(description = "Employee's email address.", example = "new.employee@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 120, message = "Password must be between 8 and 120 characters")
    @Schema(description = "Initial password for the employee.", example = "DefaultPass!123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotNull(message = "Date of birth cannot be null") // NotNull makes it required from validation POV
    @Schema(description = "Employee's date of birth.", example = "1990-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dateOfBirth;

    @NotNull(message = "Company ID cannot be null")
    @Schema(description = "ID of the company to assign the employee to.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long companyId;

    @NotEmpty(message = "Role names cannot be empty")
    @Schema(description = "List of role names to assign to the employee.", example = "[\"ROLE_EMPLOYEE\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> roleNames;

    @Schema(description = "ID of the employee's manager (optional).", example = "10", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long managerId; // Nullable
}
