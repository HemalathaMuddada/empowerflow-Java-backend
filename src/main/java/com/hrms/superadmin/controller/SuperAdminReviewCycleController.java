package com.hrms.superadmin.controller;

import com.hrms.security.service.UserDetailsImpl;
import com.hrms.superadmin.payload.request.ReviewCycleRequest;
import com.hrms.superadmin.payload.response.ReviewCycleResponseDTO;
import com.hrms.superadmin.service.SuperAdminReviewCycleService;
// Assuming local exceptions in service or common ones
import com.hrms.superadmin.service.ResourceNotFoundException;
import com.hrms.superadmin.service.BadRequestException;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/super-admin/performance/cycles")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminReviewCycleController {

    @Autowired
    private SuperAdminReviewCycleService superAdminReviewCycleService;

    @PostMapping("/")
    public ResponseEntity<ReviewCycleResponseDTO> createReviewCycle(
            @Valid @RequestBody ReviewCycleRequest request,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            ReviewCycleResponseDTO newCycle = superAdminReviewCycleService.createReviewCycle(request, superAdminUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(newCycle);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (ResourceNotFoundException ex) { // If superAdminUser for audit not found
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated super admin user not found.", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating review cycle.", ex);
        }
    }

    @PutMapping("/{cycleId}")
    public ResponseEntity<ReviewCycleResponseDTO> updateReviewCycle(
            @PathVariable Long cycleId,
            @Valid @RequestBody ReviewCycleRequest request,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            ReviewCycleResponseDTO updatedCycle = superAdminReviewCycleService.updateReviewCycle(cycleId, request, superAdminUser);
            return ResponseEntity.ok(updatedCycle);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating review cycle.", ex);
        }
    }

    @DeleteMapping("/{cycleId}")
    public ResponseEntity<Void> deleteReviewCycle(
            @PathVariable Long cycleId,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            superAdminReviewCycleService.deleteReviewCycle(cycleId, superAdminUser);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) { // For cases like "cannot delete cycle with active reviews"
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting review cycle.", ex);
        }
    }

    @GetMapping("/{cycleId}")
    public ResponseEntity<ReviewCycleResponseDTO> getReviewCycleById(@PathVariable Long cycleId) {
        try {
            ReviewCycleResponseDTO cycle = superAdminReviewCycleService.getReviewCycleById(cycleId);
            return ResponseEntity.ok(cycle);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching review cycle.", ex);
        }
    }

    @GetMapping("/")
    public ResponseEntity<Page<ReviewCycleResponseDTO>> getAllReviewCycles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate,desc") String[] sort) { // Default sort by start date descending
        try {
            List<Sort.Order> orders = new ArrayList<>();
            if (sort[0].contains(",")) {
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    if (_sort.length == 2) orders.add(new Sort.Order(Sort.Direction.fromString(_sort[1]), _sort[0]));
                    else orders.add(new Sort.Order(Sort.Direction.DESC, "startDate"));
                }
            } else {
                 if (sort.length == 2) orders.add(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
                 else orders.add(new Sort.Order(Sort.Direction.DESC, "startDate"));
            }
            Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
            Page<ReviewCycleResponseDTO> cyclesPage = superAdminReviewCycleService.getAllReviewCycles(pageable);
            return ResponseEntity.ok(cyclesPage);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching review cycles.", ex);
        }
    }
}
