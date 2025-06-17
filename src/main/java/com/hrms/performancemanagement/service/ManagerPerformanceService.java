package com.hrms.performancemanagement.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.performancemanagement.entity.PerformanceReview;
import com.hrms.performancemanagement.entity.ReviewCycle;
import com.hrms.performancemanagement.repository.PerformanceReviewRepository;
import com.hrms.employee.payload.response.PerformanceReviewDetailsDTO; // Reusing
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.hrms.audit.service.AuditLogService; // Added
import com.hrms.employee.payload.request.SelfAppraisalRequest; // Not needed, but ManagerEvaluationRequest is
import com.hrms.manager.payload.request.ManagerEvaluationRequest; // Added
import com.hrms.service.notification.EmailService; // Added
import org.springframework.security.access.AccessDeniedException; // Added
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context; // Added

import java.time.LocalDateTime; // Added
import java.util.List;
import java.util.Objects; // Added
import java.util.stream.Collectors;

@Service
public class ManagerPerformanceService {
    private static final Logger logger = LoggerFactory.getLogger(ManagerPerformanceService.class);

    @Autowired
    private PerformanceReviewRepository performanceReviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired // Added
    private AuditLogService auditLogService;

    @Autowired(required = false) // Added
    private EmailService emailService;

    // Mapper - similar to EmployeePerformanceService, consider common utility
    private PerformanceReviewDetailsDTO mapToPerformanceReviewDetailsDTO(PerformanceReview review) {
        User employee = review.getEmployee();
        User reviewer = review.getReviewer(); // This is the manager
        ReviewCycle cycle = review.getReviewCycle();

        String employeeName = (employee != null) ? employee.getFirstName() + " " + employee.getLastName() : "N/A";
        // For this context, reviewerName is the manager themselves, but for consistency:
        String reviewerName = (reviewer != null) ? reviewer.getFirstName() + " " + reviewer.getLastName() : "N/A";
        String cycleName = (cycle != null) ? cycle.getName() : "N/A";

        return new PerformanceReviewDetailsDTO(
            review.getId(),
            cycleName,
            employeeName,
            reviewerName,
            review.getGoalsAndObjectives(),
            review.getEmployeeSelfAppraisal(),
            review.getManagerEvaluation(),
            review.getEmployeeComments(),
            review.getOverallRatingByManager(),
            review.getStatus(),
            review.getSubmittedByEmployeeAt(),
            review.getReviewedByManagerAt(),
            review.getAcknowledgedByEmployeeAt(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<PerformanceReviewDetailsDTO> getReviewsForMyEvaluation(UserDetailsImpl managerUserDetails, String statusFilter) {
        User manager = userRepository.findById(managerUserDetails.getId())
            .orElseThrow(() -> new UsernameNotFoundException("Manager user not found: " + managerUserDetails.getUsername()));

        logger.debug("Fetching reviews for manager: {}, status: {}", manager.getUsername(), statusFilter);
        List<PerformanceReview> reviews = performanceReviewRepository.findByReviewerAndStatusOrderByReviewCycle_StartDateDesc(manager, statusFilter);

        return reviews.stream()
                      .map(this::mapToPerformanceReviewDetailsDTO)
                      .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PerformanceReviewDetailsDTO getReviewForEvaluation(Long reviewId, UserDetailsImpl managerUserDetails) {
        PerformanceReview review = performanceReviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Performance review not found with ID: " + reviewId));

        if (!Objects.equals(review.getReviewer().getId(), managerUserDetails.getId())) {
            logger.warn("Manager {} attempting to access review ID {} not assigned to them (assigned to manager ID {}).",
                managerUserDetails.getUsername(), reviewId, review.getReviewer().getId());
            throw new AccessDeniedException("You are not authorized to view or evaluate this performance review.");
        }
        // Optionally, add status check here if manager should only view it in certain states via this method.
        // e.g., if (!"PENDING_MANAGER_REVIEW".equalsIgnoreCase(review.getStatus())) { ... }
        return mapToPerformanceReviewDetailsDTO(review);
    }

    @Transactional
    public PerformanceReviewDetailsDTO submitManagerEvaluation(Long reviewId, ManagerEvaluationRequest request, UserDetailsImpl managerUserDetails) {
        User managerUser = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Manager user not found: " + managerUserDetails.getUsername()));

        PerformanceReview review = performanceReviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Performance review not found with ID: " + reviewId));

        if (!Objects.equals(review.getReviewer().getId(), managerUser.getId())) {
            throw new AccessDeniedException("You are not authorized to submit an evaluation for this performance review.");
        }

        if (!"PENDING_MANAGER_REVIEW".equalsIgnoreCase(review.getStatus())) {
            throw new IllegalStateException("Manager evaluation cannot be submitted at this stage. Current status: " + review.getStatus());
        }

        review.setManagerEvaluation(request.getManagerEvaluation());
        review.setOverallRatingByManager(request.getOverallRatingByManager());
        review.setReviewedByManagerAt(LocalDateTime.now());
        review.setStatus("PENDING_EMPLOYEE_ACKNOWLEDGEMENT"); // New status

        PerformanceReview savedReview = performanceReviewRepository.save(review);

        // Audit Log
        String auditDetails = String.format("Manager evaluation submitted by %s for employee %s. Rating: %d. Review Cycle: %s",
            managerUser.getUsername(), savedReview.getEmployee().getUsername(),
            savedReview.getOverallRatingByManager(), savedReview.getReviewCycle().getName());
        auditLogService.logEvent(
            managerUserDetails.getUsername(),
            managerUserDetails.getId(),
            "PERFORMANCE_REVIEW_MANAGER_EVALUATION_SUBMITTED",
            "PerformanceReview",
            String.valueOf(savedReview.getId()),
            auditDetails,
            null, "SUCCESS"
        );

        // Optional: Email Notification to Employee
        User employee = savedReview.getEmployee();
        if (emailService != null && employee != null && org.springframework.util.StringUtils.hasText(employee.getEmail())) {
            Context context = new Context();
            context.setVariable("greeting", "Dear " + employee.getFirstName() + ",");
            String emailSubject = "Performance Review Update: Manager Evaluation Completed";
            context.setVariable("subject", emailSubject);
            context.setVariable("bodyMessage",
                String.format("Your manager, %s %s, has completed their evaluation for your performance review for the '%s' cycle.<br/>" +
                              "Please log in to the HRMS portal to view the feedback and complete any further steps, such as acknowledging the review.",
                              managerUser.getFirstName(), managerUser.getLastName(), savedReview.getReviewCycle().getName()));
            // context.setVariable("actionUrl", "/employee/performance/reviews/" + savedReview.getId()); // Example for a direct link
            // context.setVariable("actionText", "View Your Review");

            try {
                emailService.sendHtmlMailFromTemplate(employee.getEmail(), emailSubject,
                                                    "manager-review-completed-employee-notification.html", context);
                logger.info("Manager evaluation completion notification sent to employee {} for Review ID {}.", employee.getEmail(), savedReview.getId());
            } catch (Exception e) {
                logger.error("Error sending manager evaluation completion notification to employee for Review ID {}: {}", savedReview.getId(), e.getMessage(), e);
            }
        } else {
             if(emailService == null) logger.warn("EmailService not configured. Skipping employee notification for manager evaluation on Review ID {}.", savedReview.getId());
             else logger.warn("Employee or employee email missing for Review ID {}. Skipping employee notification.", savedReview.getId());
        }

        return mapToPerformanceReviewDetailsDTO(savedReview);
    }
}
