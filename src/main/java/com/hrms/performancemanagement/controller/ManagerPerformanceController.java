package com.hrms.performancemanagement.controller;

import com.hrms.employee.payload.response.PerformanceReviewDetailsDTO;
import com.hrms.performancemanagement.service.ManagerPerformanceService;
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/manager/performance/reviews")
@PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_LEAD')")
public class ManagerPerformanceController {

    @Autowired
    private ManagerPerformanceService managerPerformanceService;

    @GetMapping("/pending-my-evaluation")
    public ResponseEntity<List<PerformanceReviewDetailsDTO>> getPendingManagerEvaluations(
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            // Status is hardcoded here as per task description
            List<PerformanceReviewDetailsDTO> reviews =
                managerPerformanceService.getReviewsForMyEvaluation(managerUser, "PENDING_MANAGER_REVIEW");
            return ResponseEntity.ok(reviews);
        } catch (ResourceNotFoundException ex) { // If managerUser itself is not found
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching reviews pending manager evaluation.", ex);
        }
    }

    @GetMapping("/{reviewId}/for-evaluation")
    public ResponseEntity<PerformanceReviewDetailsDTO> getPerformanceReviewForManagerAction(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            PerformanceReviewDetailsDTO reviewDetails = managerPerformanceService.getReviewForEvaluation(reviewId, managerUser);
            return ResponseEntity.ok(reviewDetails);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching performance review for evaluation.", ex);
        }
    }

    @PostMapping("/{reviewId}/manager-evaluation")
    public ResponseEntity<PerformanceReviewDetailsDTO> submitManagerEvaluation(
            @PathVariable Long reviewId,
            @Valid @RequestBody com.hrms.manager.payload.request.ManagerEvaluationRequest request,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            PerformanceReviewDetailsDTO updatedReview = managerPerformanceService.submitManagerEvaluation(reviewId, request, managerUser);
            return ResponseEntity.ok(updatedReview);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) { // For status check errors
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error submitting manager evaluation.", ex);
        }
    }
}
