package com.hrms.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema; // Added
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload containing JWT and user details upon successful login.")
public class JwtResponse {
    @Schema(description = "JSON Web Token for authentication.", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;

    @Schema(description = "Token type.", example = "Bearer")
    private String type = "Bearer";

    @Schema(description = "User ID of the authenticated user.", example = "1")
    private Long id;

    @Schema(description = "Username of the authenticated user.", example = "johndoe")
    private String username;

    @Schema(description = "Email of the authenticated user.", example = "johndoe@example.com")
    private String email;

    @Schema(description = "List of roles assigned to the authenticated user.", example = "[\"ROLE_EMPLOYEE\", \"ROLE_USER\"]")
    private List<String> roles;

    @Schema(description = "Company ID of the authenticated user (if applicable).", example = "1", nullable = true)
    private Long companyId;

    // Constructor used by AuthController, ensure fields match
    public JwtResponse(String token, Long id, String username, String email, List<String> roles, Long companyId) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.companyId = companyId;
    }
}
