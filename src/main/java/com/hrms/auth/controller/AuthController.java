package com.hrms.auth.controller;

import com.hrms.payload.request.LoginRequest;
import com.hrms.payload.response.JwtResponse;
import com.hrms.security.jwt.JwtUtil;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication Service", description = "Endpoints for user authentication and JWT token management.")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    AuditLogService auditLogService;

    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates a user and returns a JWT if successful.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful, JWT returned",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload (e.g., missing username/password)",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed (invalid credentials or account status issues)",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
    })
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails);
        try {
            String ipAddress = getClientIpAddress(request);
            auditLogService.logEvent(
                userDetails.getUsername(),
                userDetails.getId(),
                "USER_LOGIN_SUCCESS",
                "User",
                String.valueOf(userDetails.getId()),
                "User successfully logged in and JWT issued.",
                ipAddress,
                "SUCCESS"
            );
        } catch (Exception e) {
            // Consider using SLF4J logger here if available in controller
            System.err.println("Failed to log login event: " + e.getMessage());
        }

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles,
                userDetails.getCompanyId()));
    }
}
