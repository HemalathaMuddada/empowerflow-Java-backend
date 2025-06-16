package com.hrms.performancemanagement.service;

import com.hrms.audit.service.AuditLogService;
import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.performancemanagement.entity.PerformanceReview;
import com.hrms.performancemanagement.entity.ReviewCycle;
import com.hrms.performancemanagement.repository.PerformanceReviewRepository;
import com.hrms.performancemanagement.repository.ReviewCycleRepository;
import com.hrms.performancemanagement.specs.PerformanceReviewSpecification; // New Spec
import com.hrms.employee.payload.response.PerformanceReviewDetailsDTO; // Reusing
import com.hrms.hr.payload.request.InitiateReviewsRequest;
import com.hrms.hr.payload.request.HRFinalizeReviewRequest; // New DTO
import com.hrms.hr.payload.response.InitiationSummaryDTO;
import com.hrms.hr.service.ResourceNotFoundException;
import com.hrms.hr.service.BadRequestException;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.service.notification.EmailService; // For notifications
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context; // For email
import org.springframework.util.StringUtils;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HRPerformanceService {
    private static final Logger logger = LoggerFactory.getLogger(HRPerformanceService.class);

    @Autowired
    private PerformanceReviewRepository performanceReviewRepository;
    @Autowired
    private ReviewCycleRepository reviewCycleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditLogService auditLogService;
    @Autowired(required = false)
    private EmailService emailService;

    private static final Set<String> ALLOWED_CYCLE_STATUSES_FOR_INITIATION = Set.of("ACTIVE", "PLANNING");
    private static final String INITIAL_REVIEW_STATUS = "PENDING_SELF_APPRAISAL";
    private static final String FINAL_REVIEW_STATUS = "COMPLETED"; // Or "CLOSED"

    // Mapper (could be common)
    private PerformanceReviewDetailsDTO mapToPerformanceReviewDetailsDTO(PerformanceReview review) {
        User employee = review.getEmployee();
        User reviewer = review.getReviewer();
        ReviewCycle cycle = review.getReviewCycle();
        User hrFinalizer = review.getReviewedBy(); // This is the HR user who finalized

        String employeeName = (employee != null) ? employee.getFirstName() + " " + employee.getLastName() : "N/A";
        String reviewerName = (reviewer != null) ? reviewer.getFirstName() + " " + reviewer.getLastName() : "N/A";
        String cycleName = (cycle != null) ? cycle.getName() : "N/A";
        String hrFinalizerName = (hrFinalizer != null) ? hrFinalizer.getFirstName() + " " + hrFinalizer.getLastName() : null;


        return new PerformanceReviewDetailsDTO(
            review.getId(), cycleName, employeeName, reviewerName,
            review.getGoalsAndObjectives(), review.getEmployeeSelfAppraisal(),
            review.getManagerEvaluation(), review.getEmployeeComments(),
            review.getOverallRatingByManager(),
            // review.getFinalRating(), // Already in DTO
            review.getStatus(),
            review.getSubmittedByEmployeeAt(), review.getReviewedByManagerAt(),
            review.getAcknowledgedByEmployeeAt(),
            review.getCreatedAt(), review.getUpdatedAt(),
            review.getHrComments(), review.getFinalRating() // Added hrComments and finalRating
        );
    }


    @Transactional
    public InitiationSummaryDTO initiatePerformanceReviews(InitiateReviewsRequest request, UserDetailsImpl initiatorUserDetails) {
        User initiator = userRepository.findById(initiatorUserDetails.getId())
            .orElseThrow(() -> new UsernameNotFoundException("Initiator (HR user) not found: " + initiatorUserDetails.getUsername()));

        ReviewCycle reviewCycle = reviewCycleRepository.findById(request.getReviewCycleId())
            .orElseThrow(() -> new ResourceNotFoundException("Review Cycle not found with ID: " + request.getReviewCycleId()));

        if (!ALLOWED_CYCLE_STATUSES_FOR_INITIATION.contains(reviewCycle.getStatus().toUpperCase())) {
            throw new IllegalStateException(String.format("Cannot initiate reviews for cycle '%s' (ID: %d) as it is in '%s' status. Must be one of %s.",
                reviewCycle.getName(), reviewCycle.getId(), reviewCycle.getStatus(), ALLOWED_CYCLE_STATUSES_FOR_INITIATION));
        }

        List<User> employees = userRepository.findAllById(request.getEmployeeIds());
        Set<Long> foundEmployeeIds = employees.stream().map(User::getId).collect(Collectors.toSet());
        InitiationSummaryDTO summary = new InitiationSummaryDTO(request.getEmployeeIds().size());

        for (Long requestedEmployeeId : request.getEmployeeIds()) {
            if (!foundEmployeeIds.contains(requestedEmployeeId)) {
                summary.incrementEmployeeNotFoundSkipped();
                summary.addDetail(String.format("Employee with ID %d not found. Skipped.", requestedEmployeeId));
                continue;
            }
            User employee = employees.stream().filter(e -> e.getId().equals(requestedEmployeeId)).findFirst().get();
            Company initiatorCompany = initiator.getCompany();
            Company employeeCompany = employee.getCompany();
            if (initiatorCompany != null && (employeeCompany == null || !Objects.equals(initiatorCompany.getId(), employeeCompany.getId()))) {
                summary.incrementPermissionDeniedSkipped();
                summary.addDetail(String.format("HR user from company '%s' cannot initiate review for employee '%s' (ID: %d) in company '%s'. Skipped.",
                    initiatorCompany.getName(), employee.getUsername(), employee.getId(), employeeCompany != null ? employeeCompany.getName() : "N/A"));
                continue;
            }
            if (performanceReviewRepository.findByEmployeeAndReviewCycle(employee, reviewCycle).isPresent()) {
                summary.incrementAlreadyExistsSkipped();
                summary.addDetail(String.format("Review for employee '%s' (ID: %d) in cycle '%s' already exists. Skipped.",
                    employee.getUsername(), employee.getId(), reviewCycle.getName()));
                continue;
            }
            User reviewer = employee.getManager();
            if (reviewer == null) {
                summary.incrementManagerMissingSkipped();
                summary.addDetail(String.format("Employee '%s' (ID: %d) does not have a manager assigned. Cannot initiate review. Skipped.",
                    employee.getUsername(), employee.getId()));
                continue;
            }
            PerformanceReview review = new PerformanceReview();
            review.setEmployee(employee);
            review.setReviewer(reviewer);
            review.setReviewCycle(reviewCycle);
            review.setStatus(INITIAL_REVIEW_STATUS);
            PerformanceReview savedReview = performanceReviewRepository.save(review);
            summary.incrementSuccessfullyInitiated();
            summary.addDetail(String.format("Review initiated for employee '%s' (ID: %d) in cycle '%s'. Review ID: %d.",
                employee.getUsername(), employee.getId(), reviewCycle.getName(), savedReview.getId()));
            auditLogService.logEvent(
                initiatorUserDetails.getUsername(), initiator.getId(), "PERFORMANCE_REVIEW_INITIATED",
                "PerformanceReview", String.valueOf(savedReview.getId()),
                String.format("Review initiated for employee %s (ID: %d) in cycle %s (ID: %d) by %s.",
                    employee.getUsername(), employee.getId(), reviewCycle.getName(), reviewCycle.getId(), initiator.getUsername()),
                null, "SUCCESS");
        }
        return summary;
    }

    @Transactional(readOnly = true)
    public Page<PerformanceReviewDetailsDTO> getReviewsForHRAction(Long filterCompanyId, String statusFilter, UserDetailsImpl hrUserDetails, Pageable pageable) {
        User hrUser = userRepository.findById(hrUserDetails.getId())
            .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        Long effectiveCompanyId = filterCompanyId;
        if (hrUser.getCompany() != null) { // HR is company-scoped
            if (filterCompanyId == null) effectiveCompanyId = hrUser.getCompany().getId();
            else if (!Objects.equals(hrUser.getCompany().getId(), filterCompanyId))
                throw new AccessDeniedException("HR users can only view declarations for their own company.");
        } // Global HR can see for specified companyId, or all if filterCompanyId is null

        Specification<PerformanceReview> spec = PerformanceReviewSpecification.filterReviewsForHR(effectiveCompanyId, statusFilter);
        Page<PerformanceReview> reviewPage = performanceReviewRepository.findAll(spec, pageable);
        List<PerformanceReviewDetailsDTO> dtoList = reviewPage.getContent().stream()
            .map(this::mapToPerformanceReviewDetailsDTO).collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, reviewPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PerformanceReviewDetailsDTO getPerformanceReviewForHR(Long reviewId, UserDetailsImpl hrUserDetails) {
        User hrUser = userRepository.findById(hrUserDetails.getId())
            .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));
        PerformanceReview review = performanceReviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Performance Review not found with ID: " + reviewId));

        if (hrUser.getCompany() != null) { // HR is company-scoped
            User employee = review.getEmployee();
            if (employee.getCompany() == null || !Objects.equals(hrUser.getCompany().getId(), employee.getCompany().getId())) {
                throw new AccessDeniedException("HR user cannot view review for employee outside their own company.");
            }
        } // Global HR can view any review
        return mapToPerformanceReviewDetailsDTO(review);
    }

    @Transactional
    public PerformanceReviewDetailsDTO finalizeReview(Long reviewId, HRFinalizeReviewRequest request, UserDetailsImpl hrUserDetails) {
        User hrUserEntity = userRepository.findById(hrUserDetails.getId())
            .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));
        PerformanceReview review = performanceReviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Performance Review not found with ID: " + reviewId));

        // Permission Check
        if (hrUserEntity.getCompany() != null) {
            User employee = review.getEmployee();
            if (employee.getCompany() == null || !Objects.equals(hrUserEntity.getCompany().getId(), employee.getCompany().getId())) {
                throw new AccessDeniedException("HR user cannot finalize review for employee outside their own company.");
            }
        }

        if (!"PENDING_HR_REVIEW".equalsIgnoreCase(review.getStatus())) {
            throw new IllegalStateException("Review cannot be finalized by HR at this stage. Current status: " + review.getStatus());
        }

        if (request.getHrComments() != null) review.setHrComments(request.getHrComments());
        if (request.getFinalRating() != null) review.setFinalRating(request.getFinalRating());
        else if (review.getOverallRatingByManager() != null) { // If HR doesn't set final rating, default to manager's rating
            review.setFinalRating(review.getOverallRatingByManager());
        }


        review.setReviewedBy(hrUserEntity); // HR user who finalized
        review.setReviewedAt(LocalDateTime.now()); // Timestamp of HR finalization
        review.setStatus(FINAL_REVIEW_STATUS);

        PerformanceReview savedReview = performanceReviewRepository.save(review);

        auditLogService.logEvent(
            hrUserDetails.getUsername(), hrUserDetails.getId(), "PERFORMANCE_REVIEW_HR_FINALIZED",
            "PerformanceReview", String.valueOf(savedReview.getId()),
            String.format("Review ID %d for %s finalized by HR %s. Final Rating: %s. HR Comments: %s",
                savedReview.getId(), savedReview.getEmployee().getUsername(), hrUserDetails.getUsername(),
                Objects.toString(savedReview.getFinalRating(), "N/A"), Objects.toString(savedReview.getHrComments(), "N/A")),
            null, "SUCCESS");

        // Optional Email Notifications to Employee & Manager
        notifyEmployeeAndManagerOfFinalization(savedReview, hrUserEntity);

        return mapToPerformanceReviewDetailsDTO(savedReview);
    }

    private void notifyEmployeeAndManagerOfFinalization(PerformanceReview review, User hrUser) {
        if (emailService == null) {
            logger.warn("EmailService not configured. Skipping finalization notifications for Review ID {}.", review.getId());
            return;
        }

        User employee = review.getEmployee();
        User manager = review.getReviewer();
        String reviewCycleName = review.getReviewCycle().getName();
        String finalRatingStr = Objects.toString(review.getFinalRating(), "Not Specified");
        String hrCommentsStr = StringUtils.hasText(review.getHrComments()) ? review.getHrComments().replace("\n", "<br/>") : "N/A";

        // Notify Employee
        if (employee != null && StringUtils.hasText(employee.getEmail())) {
            Context empContext = new Context();
            empContext.setVariable("greeting", "Dear " + employee.getFirstName() + ",");
            String empSubject = "Performance Review Finalized for " + reviewCycleName;
            empContext.setVariable("subject", empSubject);
            empContext.setVariable("bodyMessage", String.format(
                "Your performance review for the cycle '%s' has been finalized by HR.<br/>Final Rating: %s<br/>HR Comments: <br/>%s",
                reviewCycleName, finalRatingStr, hrCommentsStr));
            empContext.setVariable("finalRating", review.getFinalRating()); // for specific template field
            empContext.setVariable("hrComments", hrCommentsStr); // for specific template field

            try {
                emailService.sendHtmlMailFromTemplate(employee.getEmail(), empSubject, "performance-review-finalized.html", empContext);
                logger.info("Finalization notification sent to employee {} for Review ID {}.", employee.getEmail(), review.getId());
            } catch (Exception e) {
                logger.error("Error sending finalization notification to employee for Review ID {}: {}", review.getId(), e.getMessage(), e);
            }
        }

        // Notify Manager
        if (manager != null && StringUtils.hasText(manager.getEmail())) {
            Context mgrContext = new Context();
            mgrContext.setVariable("greeting", "Dear " + manager.getFirstName() + ",");
            String mgrSubject = String.format("Performance Review Finalized for %s (%s)", employee.getFirstName() + " " + employee.getLastName(), reviewCycleName);
            mgrContext.setVariable("subject", mgrSubject);
            mgrContext.setVariable("bodyMessage", String.format(
                "The performance review for employee %s %s for the cycle '%s' has been finalized by HR (%s).<br/>Final Rating: %s<br/>HR Comments: <br/>%s",
                employee.getFirstName(), employee.getLastName(), reviewCycleName, hrUser.getUsername(), finalRatingStr, hrCommentsStr));
            mgrContext.setVariable("finalRating", review.getFinalRating());
            mgrContext.setVariable("hrComments", hrCommentsStr);

            try {
                emailService.sendHtmlMailFromTemplate(manager.getEmail(), mgrSubject, "performance-review-finalized.html", mgrContext);
                logger.info("Finalization notification sent to manager {} for Review ID {}.", manager.getEmail(), review.getId());
            } catch (Exception e) {
                logger.error("Error sending finalization notification to manager for Review ID {}: {}", review.getId(), e.getMessage(), e);
            }
        }
    }
}
