package com.hrms.superadmin.controller;

import com.hrms.superadmin.payload.request.CompanyCreateRequest;
import com.hrms.superadmin.payload.request.CompanyUpdateRequest;
import com.hrms.superadmin.payload.response.CompanyResponseDTO;
import com.hrms.superadmin.service.ResourceNotFoundException; // Assuming defined in SuperAdminCompanyService
import com.hrms.superadmin.service.SuperAdminCompanyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/companies")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminCompanyController {

    @Autowired
    private SuperAdminCompanyService superAdminCompanyService;

    @PostMapping("/")
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
