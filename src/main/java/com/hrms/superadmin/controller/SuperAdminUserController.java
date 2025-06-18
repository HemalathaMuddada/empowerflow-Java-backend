package com.hrms.superadmin.controller;

import com.hrms.employee.payload.response.EmployeeProfileResponse; // Reusing
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.superadmin.service.SuperAdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/super-admin/users")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminUserController {

    @Autowired
    private SuperAdminUserService superAdminUserService;

    @GetMapping("/")
    public ResponseEntity<Page<EmployeeProfileResponse>> getAllUsersGlobally(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort, // Example: "id,asc", "username,desc"
            @AuthenticationPrincipal UserDetailsImpl superAdminUser // For audit/logging if needed, not used in service logic yet
    ) {
        try {
            List<Sort.Order> orders = new ArrayList<>();
            if (sort[0].contains(",")) {
                // Multiple sort criteria
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    orders.add(new Sort.Order(Sort.Direction.fromString(_sort[1]), _sort[0]));
                }
            } else {
                // Single sort criterion
                orders.add(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
            Page<EmployeeProfileResponse> userPage = superAdminUserService.getAllUsersGlobal(
                    companyId, roleName, isActive, pageable);
            return ResponseEntity.ok(userPage);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching users", ex);
        }
    }

    @PostMapping("/users/{userId}/grant-super-admin")
    public ResponseEntity<EmployeeProfileResponse> grantSuperAdmin(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl currentSuperAdminUser) {
        try {
            EmployeeProfileResponse updatedUser = superAdminUserService.grantSuperAdminRole(userId, currentSuperAdminUser);
            return ResponseEntity.ok(updatedUser);
        } catch (ResourceNotFoundException ex) { // Assuming these exceptions are from hr.service
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error granting Super Admin role", ex);
        }
    }

    @PostMapping("/users/{userId}/revoke-super-admin")
    public ResponseEntity<EmployeeProfileResponse> revokeSuperAdmin(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl currentSuperAdminUser) {
        try {
            EmployeeProfileResponse updatedUser = superAdminUserService.revokeSuperAdminRole(userId, currentSuperAdminUser);
            return ResponseEntity.ok(updatedUser);
        } catch (ResourceNotFoundException ex) { // Assuming these exceptions are from hr.service
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error revoking Super Admin role", ex);
        }
    }

    @PostMapping("/users/{userId}/roles/assign/{roleName}")
    public ResponseEntity<EmployeeProfileResponse> assignRoleToUser(
            @PathVariable Long userId,
            @PathVariable String roleName,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            // Sanitize/validate roleName further if needed (e.g., ensure it's URL-safe or matches expected format)
            // For now, relying on service validation against MANAGEABLE_ROLES
            EmployeeProfileResponse updatedUser = superAdminUserService.assignRoleToUser(userId, roleName.toUpperCase(), superAdminUser);
            return ResponseEntity.ok(updatedUser);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error assigning role to user", ex);
        }
    }

    @PostMapping("/users/{userId}/roles/revoke/{roleName}")
    public ResponseEntity<EmployeeProfileResponse> revokeRoleFromUser(
            @PathVariable Long userId,
            @PathVariable String roleName,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            EmployeeProfileResponse updatedUser = superAdminUserService.removeRoleFromUser(userId, roleName.toUpperCase(), superAdminUser);
            return ResponseEntity.ok(updatedUser);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error revoking role from user", ex);
        }
    }
}
