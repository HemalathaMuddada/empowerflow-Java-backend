package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema; // Added

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed profile information for an employee.")
public class EmployeeProfileResponse {
    @Schema(description = "Unique ID of the employee.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Employee's first name.", example = "Jane", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Schema(description = "Employee's last name.", example = "Smith", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Schema(description = "Employee's username.", example = "janesmith", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Employee's email address.", example = "jane.smith@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Employee's date of birth.", example = "1985-05-20", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate dateOfBirth;

    @Schema(description = "Name of the company the employee belongs to.", example = "Example Corp", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String companyName;

    @Schema(description = "List of roles assigned to the employee.", example = "[\"ROLE_EMPLOYEE\", \"ROLE_LEAD\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> roles;

    @Schema(description = "Employee's job designation or title.", example = "Senior Software Engineer", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String designation;

    @Schema(description = "Current active status of the employee's account.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isActive;

    @Schema(description = "Count of open tasks assigned to the employee (relevant during offboarding).", example = "3", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long openTasksCount;

    @Schema(description = "Indicates if the employee is a manager with active direct reportees (relevant during offboarding).", example = "false", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean isManagerWithActiveReportees;

    // Offboarding specific fields
    @Schema(description = "Employee's offboarding date or last working day.", example = "2024-12-31", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate offboardingDate;

    @Schema(description = "Reason for the employee's departure.", example = "Resigned for better opportunity", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String reasonForLeaving;

    @Schema(description = "Comments from HR regarding the offboarding process.", example = "Exit interview conducted.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String offboardingCommentsByHR;
}
