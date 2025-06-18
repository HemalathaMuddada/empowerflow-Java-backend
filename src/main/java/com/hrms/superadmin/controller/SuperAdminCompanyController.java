package com.hrms.superadmin.controller;

import com.hrms.exception.ResourceNotFoundException;
import com.hrms.superadmin.payload.request.CompanyCreateRequest;
import com.hrms.superadmin.payload.request.CompanyUpdateRequest;
import com.hrms.superadmin.payload.response.CompanyResponseDTO;
import com.hrms.superadmin.service.SuperAdminCompanyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation; // Added
import io.swagger.v3.oas.annotations.Parameter; // Added
import io.swagger.v3.oas.annotations.media.ArraySchema; // Added
import io.swagger.v3.oas.annotations.media.Content; // Added
import io.swagger.v3.oas.annotations.media.Schema; // Added
import io.swagger.v3.oas.annotations.responses.ApiResponse; // Added
import io.swagger.v3.oas.annotations.responses.ApiResponses; // Added
import io.swagger.v3.oas.annotations.tags.Tag; // Added
import com.hrms.superadmin.payload.response.CompanyResponseDTO; // For schema link
import java.util.List;

@RestController
@RequestMapping("/api/super-admin/companies")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
@Tag(name = "Super Admin - Company Management", description = "Endpoints for Super Admins to manage company records.")
public class SuperAdminCompanyController {

    @Autowired
    private SuperAdminCompanyService superAdminCompanyService;

    @PostMapping("/")
    @Operation(summary = "Create New Company", description = "Allows Super Admin to create a new company.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Company created successfully",
                     content = @Content(schema = @Schema(implementation = CompanyResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid company data (e.g., name missing)", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden (User does not have ROLE_SUPER_ADMIN)", content = @Content),
        @ApiResponse(responseCode = "409", description = "Conflict (e.g., company name already exists)", content = @Content)
    })
    public ResponseEntity<CompanyResponseDTO> createCompany(@Valid @RequestBody CompanyCreateRequest request) {
        try {
            CompanyResponseDTO newCompany = superAdminCompanyService.createCompany(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(newCompany);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating company", ex);
        }
    }

    @PatchMapping("/{companyId}")
    public ResponseEntity<CompanyResponseDTO> updateCompany(
            @PathVariable Long companyId,
            @Valid @RequestBody CompanyUpdateRequest request) {
        try {
            CompanyResponseDTO updatedCompany = superAdminCompanyService.updateCompany(companyId, request);
            return ResponseEntity.ok(updatedCompany);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating company", ex);
        }
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyResponseDTO> getCompanyById(@PathVariable Long companyId) {
        try {
            CompanyResponseDTO company = superAdminCompanyService.getCompanyById(companyId);
            return ResponseEntity.ok(company);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching company", ex);
        }
    }

    @GetMapping("/")
    @Operation(summary = "List All Companies", description = "Allows Super Admin to retrieve a list of all companies, with optional filtering by active status.")
    @Parameter(name = "isActive", description = "Filter companies by active status (true/false).", required = false, example = "true")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of companies",
                     content = @Content(array = @ArraySchema(schema = @Schema(implementation = CompanyResponseDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<List<CompanyResponseDTO>> getAllCompanies(
            @RequestParam(required = false) Boolean isActive) {
        try {
            List<CompanyResponseDTO> companies = superAdminCompanyService.getAllCompanies(isActive);
            return ResponseEntity.ok(companies);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching companies", ex);
        }
    }
}
