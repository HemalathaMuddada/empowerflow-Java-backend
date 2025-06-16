package com.hrms.manager.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerActionHRUserRequest {

    @NotBlank(message = "First name cannot be blank")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50)
    private String username; // Required for add, cannot be changed on update via this DTO

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(max = 100)
    private String email;

    // Password: Required for add, optional for update.
    // If blank on update, it's not changed.
    @Size(min = 8, max = 120, message = "Password must be between 8 and 120 characters if provided")
    private String password;

    private LocalDate dateOfBirth; // Optional

    private Boolean isActive;      // Optional, defaults to true on add
}
