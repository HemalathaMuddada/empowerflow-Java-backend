package com.hrms.payload.request;

import io.swagger.v3.oas.annotations.media.Schema; // Added
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for user login.")
public class LoginRequest {

    @NotBlank(message = "Username cannot be blank")
    @Schema(description = "User's username.", example = "johndoe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Schema(description = "User's password.", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
