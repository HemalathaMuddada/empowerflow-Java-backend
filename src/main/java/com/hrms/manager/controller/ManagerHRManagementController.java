package com.hrms.manager.controller;

import com.hrms.employee.payload.response.EmployeeProfileResponse; // Reusing
import com.hrms.manager.payload.request.ManagerActionHRUserRequest;
import com.hrms.manager.payload.request.UserStatusUpdateRequest;
import com.hrms.manager.service.ManagerUserService;
import com.hrms.manager.service.BadRequestException; // Assuming defined in ManagerUserService.java
import com.hrms.manager.service.ResourceNotFoundException; // Assuming defined in ManagerUserService.java
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

@RestController
@RequestMapping("/api/manager/hr-users")
@PreAuthorize("hasRole('ROLE_MANAGER')")
public class ManagerHRManagementController {

    @Autowired
    private ManagerUserService managerUserService;

    @PostMapping("/add")
    public ResponseEntity<EmployeeProfileResponse> addHRUser(
            @Valid @RequestBody ManagerActionHRUserRequest request,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            EmployeeProfileResponse newHrUser = managerUserService.addHRUser(request, managerUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(newHrUser);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex);
        }
    }

    @PatchMapping("/{hrUserId}/update")
    public ResponseEntity<EmployeeProfileResponse> updateHRUser(
            @PathVariable Long hrUserId,
            @Valid @RequestBody ManagerActionHRUserRequest request,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            EmployeeProfileResponse updatedHrUser = managerUserService.updateHRUser(hrUserId, request, managerUser);
            return ResponseEntity.ok(updatedHrUser);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex);
        }
    }

    @PatchMapping("/{hrUserId}/status")
    public ResponseEntity<EmployeeProfileResponse> toggleHRUserActiveStatus(
            @PathVariable Long hrUserId,
            @Valid @RequestBody UserStatusUpdateRequest statusRequest,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            EmployeeProfileResponse updatedHrUser = managerUserService.toggleHRUserActiveStatus(
                    hrUserId, statusRequest.getIsActive(), managerUser);
            return ResponseEntity.ok(updatedHrUser);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex);
        }
    }
}
