package com.hrms.employee.controller;

import com.hrms.employee.payload.request.InvestmentDeclarationRequest;
import com.hrms.employee.payload.response.InvestmentDeclarationDTO;
import com.hrms.employee.service.EmployeeInvestmentService;
import com.hrms.hr.service.BadRequestException; // Assuming accessible
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/employee/investments/declarations")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')") // Or just ROLE_EMPLOYEE if strictly personal
public class EmployeeInvestmentController {

    @Autowired
    private EmployeeInvestmentService employeeInvestmentService;

    @PostMapping("/")
    public ResponseEntity<InvestmentDeclarationDTO> submitNewDeclaration(
            @Valid @RequestPart("declaration") InvestmentDeclarationRequest request,
            @RequestPart(value = "proofDocument", required = false) MultipartFile proofDocumentFile,
            @AuthenticationPrincipal UserDetailsImpl employeeUser) {
        try {
            InvestmentDeclarationDTO declarationDTO = employeeInvestmentService.submitDeclaration(request, proofDocumentFile, employeeUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(declarationDTO);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (IOException ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process file upload.", ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", ex);
        }
    }

    @GetMapping("/my-declarations")
    public ResponseEntity<List<InvestmentDeclarationDTO>> getMyInvestmentDeclarations(
            @AuthenticationPrincipal UserDetailsImpl employeeUser) {
        try {
            List<InvestmentDeclarationDTO> declarations = employeeInvestmentService.getMyDeclarations(employeeUser);
            return ResponseEntity.ok(declarations);
        } catch (ResourceNotFoundException ex) { // If employeeUser themselves not found by service
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching investment declarations.", ex);
        }
    }
}
