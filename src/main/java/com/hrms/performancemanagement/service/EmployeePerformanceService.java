package com.hrms.performancemanagement.service;

import com.hrms.audit.service.AuditLogService;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.performancemanagement.entity.PerformanceReview;
import com.hrms.performancemanagement.entity.ReviewCycle;
import com.hrms.performancemanagement.repository.PerformanceReviewRepository;
import com.hrms.employee.payload.request.SelfAppraisalRequest;
import com.hrms.employee.payload.response.PerformanceReviewDetailsDTO;
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible
// import com.hrms.hr.service.BadRequestException; // Or use IllegalStateException
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import com.hrms.service.notification.EmailService; // Added for optional email
import org.springframework.util.StringUtils; // Added for StringUtils
import org.thymeleaf.context.Context; // Added for Thymeleaf context

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EmployeePerformanceService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeePerformanceService.class);

    @Autowired
    private PerformanceReviewRepository performanceReviewRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditLogService auditLogService;

    @Autowired(required = false) // Added EmailService
    private EmailService emailService;

    private PerformanceReviewDetailsDTO mapToPerformanceReviewDetailsDTO(PerformanceReview review) {
        User employee = review.getEmployee();
        User reviewer = review.getReviewer();
        ReviewCycle cycle = review.getReviewCycle();

        String employeeName = (employee != null) ? employee.getFirstName() + " " + employee.getLastName() : "N/A";
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
    public List<PerformanceReviewDetailsDTO> getMyPerformanceReviews(UserDetailsImpl employeeUserDetails) {
        User employee = userRepository.findById(employeeUserDetails.getId())
            .orElseThrow(() -> new UsernameNotFoundException("Employee not found: " + employeeUserDetails.getUsername()));

        List<PerformanceReview> reviews = performanceReviewRepository.findByEmployeeOrderByReviewCycle_StartDateDesc(employee);
        return reviews.stream().map(this::mapToPerformanceReviewDetailsDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PerformanceReviewDetailsDTO getPerformanceReviewDetails(Long reviewId, UserDetailsImpl employeeUserDetails) {
        PerformanceReview review = performanceReviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Performance review not found with ID: " + reviewId));

        if (!Objects.equals(review.getEmployee().getId(), employeeUserDetails.getId())) {
            // Allow manager to view if they are the reviewer for this review.
            // Or if HR has specific permissions (would need role check)
            // For now, strictly employee owns their view, unless it's their manager viewing.
            boolean isReviewer = Objects.equals(review.getReviewer().getId(), employeeUserDetails.getId());
            // boolean isHR = employeeUserDetails.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_HR"));

            // For this service (EmployeePerformanceService), only employee can view. Managers/HR use their own services/controllers.
            if(!isReviewer) { // This check for reviewer is more for a combined service. Here, it's employee only.
                 throw new AccessDeniedException("You are not authorized to view this performance review.");
            }
            // Correction: This service is EmployeePerformanceService, so only employee should view.
            // The above 'isReviewer' check would be for a more general service.
            // For strict employee-only access:
            // if (!Objects.equals(review.getEmployee().getId(), employeeUserDetails.getId())) {
            //    throw new AccessDeniedException("You are not authorized to view this performance review.");
            // }
        }
        // Re-evaluating the access for getPerformanceReviewDetails for an employee:
        // An employee should only be able to get their own review.
        if (!Objects.equals(review.getEmployee().getId(), employeeUserDetails.getId())) {
            logger.warn("User {} attempting to access review ID {} belonging to user {}",
                employeeUserDetails.getUsername(), reviewId, review.getEmployee().getUsername());
            throw new AccessDeniedException("You are not authorized to view this performance review.");
        }


        return mapToPerformanceReviewDetailsDTO(review);
    }

    @Transactional
    public PerformanceReviewDetailsDTO submitSelfAppraisal(Long reviewId, SelfAppraisalRequest request, UserDetailsImpl employeeUserDetails) {
        PerformanceReview review = performanceReviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Performance review not found with ID: " + reviewId));

        if (!Objects.equals(review.getEmployee().getId(), employeeUserDetails.getId())) {
            throw new AccessDeniedException("You can only submit self-appraisal for your own performance review.");
        }

        if (!"PENDING_SELF_APPRAISAL".equalsIgnoreCase(review.getStatus())) {
            throw new IllegalStateException("Self-appraisal cannot be submitted at this stage. Current status: " + review.getStatus());
        }

        review.setGoalsAndObjectives(request.getGoalsAndObjectives());
        review.setEmployeeSelfAppraisal(request.getEmployeeSelfAppraisal());
        review.setSubmittedByEmployeeAt(LocalDateTime.now());
        review.setStatus("PENDING_MANAGER_REVIEW"); // Update status

        PerformanceReview savedReview = performanceReviewRepository.save(review);

        auditLogService.logEvent(
            employeeUserDetails.getUsername(),
            employeeUserDetails.getId(),
            "PERFORMANCE_REVIEW_SELF_APPRAISAL_SUBMITTED",
            "PerformanceReview",
            String.valueOf(savedReview.getId()),
            String.format("Self-appraisal submitted by %s for review cycle %s.",
                          employeeUserDetails.getUsername(), review.getReviewCycle().getName()),
            null, "SUCCESS"
        );

        return mapToPerformanceReviewDetailsDTO(savedReview);
    }

    @Transactional
    public PerformanceReviewDetailsDTO acknowledgeManagerReview(Long reviewId, EmployeeFinalCommentsRequest request, UserDetailsImpl employeeUserDetails) {
        PerformanceReview review = performanceReviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Performance review not found with ID: " + reviewId));

        if (!Objects.equals(review.getEmployee().getId(), employeeUserDetails.getId())) {
            throw new AccessDeniedException("You can only acknowledge your own performance review.");
        }

        if (!"PENDING_EMPLOYEE_ACKNOWLEDGEMENT".equalsIgnoreCase(review.getStatus())) {
            throw new IllegalStateException("Review cannot be acknowledged at this stage. Current status: " + review.getStatus());
        }

        if (request != null && StringUtils.hasText(request.getEmployeeComments())) {
            review.setEmployeeComments(request.getEmployeeComments());
        }
        review.setAcknowledgedByEmployeeAt(LocalDateTime.now());
        review.setStatus("PENDING_HR_REVIEW"); // New status

        PerformanceReview savedReview = performanceReviewRepository.save(review);

        // Audit Log
        String auditDetails = String.format("Employee %s acknowledged manager's review for cycle %s. Comments added: %s",
                                            employeeUserDetails.getUsername(), review.getReviewCycle().getName(),
                                            (request != null && StringUtils.hasText(request.getEmployeeComments())) ? "Yes" : "No");
        auditLogService.logEvent(
            employeeUserDetails.getUsername(),
            employeeUserDetails.getId(),
            "PERFORMANCE_REVIEW_EMPLOYEE_ACKNOWLEDGED",
            "PerformanceReview",
            String.valueOf(savedReview.getId()),
            auditDetails,
            null, "SUCCESS"
        );

        // Optional: Email Notification to Manager
        User manager = savedReview.getReviewer();
        User employee = savedReview.getEmployee(); // employeeUserDetails can also be used for name
        if (emailService != null && manager != null && StringUtils.hasText(manager.getEmail())) {
            Context context = new Context();
            String managerName = manager.getFirstName();
            String employeeName = employee.getFirstName() + " " + employee.getLastName();
            String reviewCycleName = savedReview.getReviewCycle().getName();

            context.setVariable("greeting", "Dear " + managerName + ",");
            String emailSubject = "Performance Review Acknowledged by " + employeeName;
            context.setVariable("subject", emailSubject);
            String bodyMsg = String.format("%s has acknowledged their performance review for the '%s' cycle.",
                                           employeeName, reviewCycleName);
            context.setVariable("bodyMessage", bodyMsg);
            context.setVariable("employeeComments", StringUtils.hasText(savedReview.getEmployeeComments()) ? savedReview.getEmployeeComments().replace("\n", "<br/>") : null);


            try {
                emailService.sendHtmlMailFromTemplate(manager.getEmail(), emailSubject,
                                                    "employee-review-acknowledged.html", context);
                logger.info("Employee acknowledgement notification sent to manager {} for Review ID {}.", manager.getEmail(), savedReview.getId());
            } catch (Exception e) {
                logger.error("Error sending employee acknowledgement notification to manager for Review ID {}: {}", savedReview.getId(), e.getMessage(), e);
            }
        } else {
            if(emailService == null) logger.warn("EmailService not configured. Skipping manager notification for employee acknowledgement on Review ID {}.", savedReview.getId());
            else logger.warn("Manager or manager email missing for Review ID {}. Skipping manager notification.", savedReview.getId());
        }

        return mapToPerformanceReviewDetailsDTO(savedReview);
    }
}
