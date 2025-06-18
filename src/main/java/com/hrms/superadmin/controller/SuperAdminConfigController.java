package com.hrms.superadmin.controller;

import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.superadmin.payload.request.SystemConfigurationCreateRequest;
import com.hrms.superadmin.payload.request.SystemConfigurationUpdateRequest;
import com.hrms.superadmin.payload.response.SystemConfigurationDTO;
import com.hrms.superadmin.service.SuperAdminConfigService;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/configurations")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminConfigController {

    @Autowired
    private SuperAdminConfigService superAdminConfigService;

    @GetMapping("/")
    public ResponseEntity<List<SystemConfigurationDTO>> getAllSystemConfigurations() {
        try {
            List<SystemConfigurationDTO> configs = superAdminConfigService.getAllConfigurations();
            return ResponseEntity.ok(configs);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching all configurations", ex);
        }
    }

    @GetMapping("/{configKey}")
    public ResponseEntity<SystemConfigurationDTO> getSystemConfigurationByKey(@PathVariable String configKey) {
        try {
            SystemConfigurationDTO config = superAdminConfigService.getConfigurationByKey(configKey);
            return ResponseEntity.ok(config);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching configuration by key", ex);
        }
    }

    @PostMapping("/")
    public ResponseEntity<SystemConfigurationDTO> createSystemConfiguration(
            @Valid @RequestBody SystemConfigurationCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            SystemConfigurationDTO newConfig = superAdminConfigService.createConfiguration(request, superAdminUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(newConfig);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (ResourceNotFoundException ex) { // If superAdminUser not found in service
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated super admin user not found.", ex);
        }
        catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating configuration", ex);
        }
    }

    @PutMapping("/{configKey}")
    public ResponseEntity<SystemConfigurationDTO> updateSystemConfiguration(
            @PathVariable String configKey,
            @Valid @RequestBody SystemConfigurationUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            SystemConfigurationDTO updatedConfig = superAdminConfigService.updateConfiguration(configKey, request, superAdminUser);
            return ResponseEntity.ok(updatedConfig);
        } catch (ResourceNotFoundException ex) {
            // Can be either config key not found or superAdminUser not found
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating configuration", ex);
        }
    }
}
