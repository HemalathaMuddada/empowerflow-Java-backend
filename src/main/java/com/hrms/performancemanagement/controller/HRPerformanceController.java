package com.hrms.performancemanagement.controller;

import com.hrms.hr.payload.request.InitiateReviewsRequest;
import com.hrms.hr.payload.response.InitiationSummaryDTO;
import com.hrms.performancemanagement.service.HRPerformanceService;
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible
import com.hrms.hr.service.BadRequestException; // Assuming accessible
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

@RestController
@RequestMapping("/api/hr/performance/reviews")
// Class-level for HR, specific endpoints can add Manager if they can view some things
@PreAuthorize("hasRole('ROLE_HR')")
public class HRPerformanceController {

    @Autowired
    private HRPerformanceService hrPerformanceService;

    @PostMapping("/initiate")
    public ResponseEntity<InitiationSummaryDTO> initiateReviews(
            @Valid @RequestBody InitiateReviewsRequest request,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            InitiationSummaryDTO summary = hrPerformanceService.initiatePerformanceReviews(request, hrUser);
            if (summary.getSuccessfullyInitiatedCount() == 0 && summary.getTotalRequested() > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(summary);
            } else if (summary.getSuccessfullyInitiatedCount() < summary.getTotalRequested()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(summary);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(summary);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error initiating performance reviews.", ex);
        }
    }

    @GetMapping("/for-hr-action")
    public ResponseEntity<Page<PerformanceReviewDetailsDTO>> getReviewsForHRAction(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false, defaultValue = "PENDING_HR_REVIEW") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "reviewCycle.startDate,desc") String[] sort,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            List<Sort.Order> orders = new ArrayList<>();
            if (sort[0].contains(",")) {
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    if (_sort.length == 2) orders.add(new Sort.Order(Sort.Direction.fromString(_sort[1]), _sort[0]));
                    else orders.add(new Sort.Order(Sort.Direction.DESC, "reviewCycle.startDate"));
                }
            } else {
                 if (sort.length == 2) orders.add(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
                 else orders.add(new Sort.Order(Sort.Direction.DESC, "reviewCycle.startDate"));
            }
            Pageable pageable = PageRequest.of(page, size, Sort.by(orders));

            Page<PerformanceReviewDetailsDTO> reviewsPage = hrPerformanceService.getReviewsForHRAction(companyId, status, hrUser, pageable);
            return ResponseEntity.ok(reviewsPage);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (BadRequestException | ResourceNotFoundException ex) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching reviews for HR action.", ex);
        }
    }

    @GetMapping("/{reviewId}/for-hr-view")
    public ResponseEntity<PerformanceReviewDetailsDTO> getPerformanceReviewForHR(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            PerformanceReviewDetailsDTO reviewDetails = hrPerformanceService.getPerformanceReviewForHR(reviewId, hrUser);
            return ResponseEntity.ok(reviewDetails);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching performance review details for HR.", ex);
        }
    }

    @PostMapping("/{reviewId}/hr-finalize")
    public ResponseEntity<PerformanceReviewDetailsDTO> finalizePerformanceReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody HRFinalizeReviewRequest request,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            PerformanceReviewDetailsDTO finalizedReview = hrPerformanceService.finalizeReview(reviewId, request, hrUser);
            return ResponseEntity.ok(finalizedReview);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error finalizing performance review.", ex);
        }
    }
}
