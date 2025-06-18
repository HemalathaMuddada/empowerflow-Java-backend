package com.hrms.performancemanagement.controller;

import com.hrms.employee.payload.request.EmployeeFinalCommentsRequest;
import com.hrms.employee.payload.request.SelfAppraisalRequest;
import com.hrms.employee.payload.response.PerformanceReviewDetailsDTO;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.performancemanagement.service.EmployeePerformanceService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/employee/performance/reviews")
// All employee-like roles can view their reviews. Specific actions might have tighter checks in service.
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class EmployeePerformanceController {

    @Autowired
    private EmployeePerformanceService employeePerformanceService;

    @GetMapping("/")
    public ResponseEntity<List<PerformanceReviewDetailsDTO>> getMyPerformanceReviews(
            @AuthenticationPrincipal UserDetailsImpl employeeUser) {
        try {
            List<PerformanceReviewDetailsDTO> reviews = employeePerformanceService.getMyPerformanceReviews(employeeUser);
            return ResponseEntity.ok(reviews);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching performance reviews.", ex);
        }
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<PerformanceReviewDetailsDTO> getPerformanceReviewDetails(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetailsImpl employeeUser) {
        try {
            PerformanceReviewDetailsDTO reviewDetails = employeePerformanceService.getPerformanceReviewDetails(reviewId, employeeUser);
            return ResponseEntity.ok(reviewDetails);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching performance review details.", ex);
        }
    }

    @PostMapping("/{reviewId}/self-appraisal")
    public ResponseEntity<PerformanceReviewDetailsDTO> submitSelfAppraisal(
            @PathVariable Long reviewId,
            @Valid @RequestBody SelfAppraisalRequest request,
            @AuthenticationPrincipal UserDetailsImpl employeeUser) {
        try {
            PerformanceReviewDetailsDTO updatedReview = employeePerformanceService.submitSelfAppraisal(reviewId, request, employeeUser);
            return ResponseEntity.ok(updatedReview); // Or HttpStatus.CREATED if it's the first submission
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) { // For status check errors
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error submitting self-appraisal.", ex);
        }
    }

    @PostMapping("/{reviewId}/acknowledge")
    public ResponseEntity<PerformanceReviewDetailsDTO> acknowledgeReview(
            @PathVariable Long reviewId,
            @RequestBody(required = false) EmployeeFinalCommentsRequest request, // Comments are optional
            @AuthenticationPrincipal UserDetailsImpl employeeUser) {
        try {
            // If request is null (empty body sent for just acknowledgement), create an empty request object
            EmployeeFinalCommentsRequest actualRequest = (request == null) ? new EmployeeFinalCommentsRequest() : request;
            PerformanceReviewDetailsDTO updatedReview = employeePerformanceService.acknowledgeManagerReview(reviewId, actualRequest, employeeUser);
            return ResponseEntity.ok(updatedReview);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error acknowledging review.", ex);
        }
    }
}
