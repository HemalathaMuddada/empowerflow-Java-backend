package com.hrms.hr.controller;

import com.hrms.core.repository.UserRepository; // Added for /unassigned check
import com.hrms.employee.payload.response.EmployeeProfileResponse;
import com.hrms.hr.payload.request.HRAddEmployeeRequest;
import com.hrms.hr.payload.request.HRUpdateDesignationRequest;
import com.hrms.hr.payload.request.HRUpdateEmployeeRequest;
import com.hrms.hr.payload.request.InitiateOffboardingRequest; // New
import com.hrms.hr.payload.request.ReassignReporteesRequest;
import com.hrms.hr.payload.response.ReporteeReassignmentResponse;
import com.hrms.hr.service.HREmployeeService;
import com.hrms.hr.service.ResourceNotFoundException;
import com.hrms.hr.service.BadRequestException;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/hr/employees")
@PreAuthorize("hasAnyRole('ROLE_HR', 'ROLE_MANAGER')")
public class HREmployeeController {

    @Autowired
    private HREmployeeService hrEmployeeService;

    // Needs UserRepository injected for the /unassigned endpoint's direct check
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/add")
    public ResponseEntity<EmployeeProfileResponse> addNewEmployee(
            @Valid @RequestBody HRAddEmployeeRequest addEmployeeRequest,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            EmployeeProfileResponse newEmployee = hrEmployeeService.addNewEmployee(addEmployeeRequest, hrUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(newEmployee);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex);
        }
    }

    @PatchMapping("/{employeeId}/update-details")
    public ResponseEntity<EmployeeProfileResponse> updateEmployeeDetails(
            @PathVariable Long employeeId,
            @Valid @RequestBody HRUpdateEmployeeRequest updateRequest,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            EmployeeProfileResponse updatedEmployee = hrEmployeeService.updateEmployeeDetails(employeeId, updateRequest, hrUser);
            return ResponseEntity.ok(updatedEmployee);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) { // Added AccessDeniedException
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        }
         catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during update", ex);
        }
    }

    @PatchMapping("/{employeeId}/designation")
    public ResponseEntity<EmployeeProfileResponse> changeEmployeeDesignation(
            @PathVariable Long employeeId,
            @Valid @RequestBody HRUpdateDesignationRequest designationRequest,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            EmployeeProfileResponse updatedEmployee = hrEmployeeService.changeEmployeeDesignation(employeeId, designationRequest, hrUser);
            return ResponseEntity.ok(updatedEmployee);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) { // Added AccessDeniedException
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        }
         catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex);
        }
    }

    @PatchMapping("/{employeeId}/initiate-offboarding") // Renamed path from /deactivate
    public ResponseEntity<EmployeeProfileResponse> initiateEmployeeOffboarding( // Renamed method
            @PathVariable Long employeeId,
            @Valid @RequestBody InitiateOffboardingRequest offboardingRequest, // New DTO
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            EmployeeProfileResponse updatedEmployee = hrEmployeeService.initiateOffboarding(employeeId, offboardingRequest, hrUser);
            return ResponseEntity.ok(updatedEmployee);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during offboarding initiation.", ex);
        }
    }

    @GetMapping("/by-company/{companyId}")
    public ResponseEntity<List<EmployeeProfileResponse>> getEmployeesByCompany(
            @PathVariable Long companyId,
            @RequestParam(required = false) Boolean isActive,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            List<EmployeeProfileResponse> employees = hrEmployeeService.getEmployeesByCompanyAndStatus(companyId, isActive, hrUser);
            return ResponseEntity.ok(employees);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex);
        }
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('ROLE_HR')")
    public ResponseEntity<List<EmployeeProfileResponse>> getUnassignedEmployees(
            @RequestParam(required = false) Boolean isActive,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            com.hrms.core.entity.User authenticatedHrUserEntity = userRepository.findById(hrUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated HR User not found"));
            if (authenticatedHrUserEntity.getCompany() != null) {
                 throw new AccessDeniedException("This operation is only allowed for global HR administrators.");
            }
            List<EmployeeProfileResponse> employees = hrEmployeeService.getEmployeesByCompanyAndStatus(null, isActive, hrUser);
            return ResponseEntity.ok(employees);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex);
        }
    }

    @PostMapping("/reassign-direct-reportees")
    @PreAuthorize("hasRole('ROLE_HR')")
    public ResponseEntity<ReporteeReassignmentResponse> reassignDirectReportees(
            @Valid @RequestBody ReassignReporteesRequest request,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            ReporteeReassignmentResponse response = hrEmployeeService.reassignDirectReportees(request, hrUser);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during reassignment.", ex);
        }
    }
}
