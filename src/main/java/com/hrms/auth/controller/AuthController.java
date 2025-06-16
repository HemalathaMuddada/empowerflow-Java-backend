package com.hrms.auth.controller;

import com.hrms.payload.request.LoginRequest;
import com.hrms.payload.response.JwtResponse;
import com.hrms.security.jwt.JwtUtil;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.audit.service.AuditLogService; // Added AuditLogService
import jakarta.servlet.http.HttpServletRequest; // Added HttpServletRequest
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

@CrossOrigin(origins = "*", maxAge = 3600) // Configure origins as needed
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    AuditLogService auditLogService; // Injected AuditLogService

    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) { // Added HttpServletRequest

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails); // Token generation might vary

        // Audit Log Call
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
            // Log audit logging failure but do not fail the login
            // Logger should be added to this controller for this kind of internal logging
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
