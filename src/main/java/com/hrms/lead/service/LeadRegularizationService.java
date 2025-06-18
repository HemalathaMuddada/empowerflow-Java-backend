package com.hrms.lead.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Attendance;
import com.hrms.employee.core.entity.RegularizationRequest;
import com.hrms.employee.core.enums.RegularizationStatus;
import com.hrms.employee.core.repository.RegularizationRequestRepository;
import com.hrms.lead.payload.response.PendingTeamRegularizationResponse;
import com.hrms.employee.core.entity.Attendance;
import com.hrms.employee.core.enums.RegularizationStatus;
import com.hrms.employee.core.repository.AttendanceRepository; // Needed for saving Attendance
import com.hrms.lead.payload.request.RegularizationActionRequest;
import com.hrms.lead.payload.response.TeamRegularizationRequestDTO;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.service.notification.EmailService; // Added EmailService
import org.slf4j.Logger; // Added Logger
import org.slf4j.LoggerFactory; // Added Logger
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LeadRegularizationService {

    private static final Logger logger = LoggerFactory.getLogger(LeadRegularizationService.class); // Added Logger

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegularizationRequestRepository regularizationRequestRepository;

    @Autowired
    private AttendanceRepository attendanceRepository; // To update Attendance record

    @Autowired(required = false) // Make EmailService optional
    private EmailService emailService;

    @Transactional(readOnly = true)
    public PendingTeamRegularizationResponse getPendingRegularizationRequestsForMyTeam(UserDetailsImpl leadUserDetails) {
        Long leadUserId = leadUserDetails.getId();
        List<User> reportees = userRepository.findByManagerId(leadUserId);

        if (reportees.isEmpty()) {
            return new PendingTeamRegularizationResponse(Collections.emptyList());
        }

        List<RegularizationRequest> pendingRequests = regularizationRequestRepository.findByEmployeeInAndStatus(reportees, RegularizationStatus.PENDING);

        List<TeamRegularizationRequestDTO> dtoList = pendingRequests.stream()
                .map(this::mapToTeamRegularizationRequestDTO)
                .collect(Collectors.toList());

        return new PendingTeamRegularizationResponse(dtoList);
    }

    @Transactional
    public TeamRegularizationRequestDTO approveOrRejectRegularization(Long regularizationRequestId,
                                                                    RegularizationActionRequest actionRequest,
                                                                    UserDetailsImpl leadUserDetails) {
        RegularizationRequest request = regularizationRequestRepository.findById(regularizationRequestId)
                .orElseThrow(() -> new RuntimeException("Regularization request not found with id: " + regularizationRequestId)); // Or ResourceNotFoundException

        User employee = request.getEmployee();
        if (employee.getManager() == null || !Objects.equals(employee.getManager().getId(), leadUserDetails.getId())) {
            throw new AccessDeniedException("You are not authorized to action this regularization request.");
        }

        if (request.getStatus() != RegularizationStatus.PENDING) {
            throw new IllegalStateException("Regularization request is not in PENDING state. Current state: " + request.getStatus());
        }

        User leadUserEntity = userRepository.findById(leadUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Lead user not found: " + leadUserDetails.getUsername()));

        String action = actionRequest.getAction().toUpperCase();
        switch (action) {
            case "APPROVE":
                request.setStatus(RegularizationStatus.APPROVED);
                if (request.getAttendanceRecord() != null) {
                    Attendance attendance = request.getAttendanceRecord();
                    attendance.setRegularized(true);
                    // Potentially re-calculate totalHours if login/logout times were also adjusted by this regularization.
                    // For now, just marking as regularized.
                    attendanceRepository.save(attendance);
                }
                break;
            case "REJECT":
                request.setStatus(RegularizationStatus.REJECTED);
                break;
            // Default case for invalid action string is covered by @Pattern on DTO
        }

        request.setApproverComment(actionRequest.getApproverComment());
        request.setApprovedBy(leadUserEntity);
        RegularizationRequest updatedRequest = regularizationRequestRepository.save(request);

        // Send email notification to Employee (existing logic)
        if (emailService != null) {
            User employeeForEmail = userRepository.findById(employee.getId()).orElse(null);
            User leadForEmail = userRepository.findById(leadUserDetails.getId()).orElse(null);

            if (employeeForEmail != null && employeeForEmail.getEmail() != null && leadForEmail != null) {
                String employeeEmail = employeeForEmail.getEmail();
                String employeeName = employeeForEmail.getFirstName();
                String leadName = leadForEmail.getFirstName() + " " + leadForEmail.getLastName();
                String statusString = updatedRequest.getStatus().name();
                String subject = "Regularization Request " + statusString;
                String reasonDetail = updatedRequest.getReasonType().toString() +
                                      (updatedRequest.getCustomReason() != null ? " - " + updatedRequest.getCustomReason() : "");

                String textBody = String.format(
                    "Dear %s,\n\n" +
                    "Your attendance regularization request for date %s (Reason: %s) has been %s by %s.\n" +
                    "Approver Comment: %s\n\n" +
                    "You can view the details in the employee portal.\n\n" +
                    "Regards,\nHRMS Notification System",
                    employeeName,
                    updatedRequest.getRequestDate().toString(),
                    reasonDetail,
                    statusString.toLowerCase(),
                    leadName,
                    updatedRequest.getApproverComment() != null ? updatedRequest.getApproverComment() : "N/A"
                );
                // For employee, using existing simple mail or a specific template if created
                // emailService.sendSimpleMail(employeeEmail, subject, textBody);

                // Using generic template for employee for now, assuming one exists.
                // Or stick to simple mail if no generic employee template for this.
                // For this task, the focus is HR notification. We'll assume employee notification part is okay.
                // The original objective didn't ask to change employee notification, but to add HR one.
                // So, let's keep the original employee notification logic if it was simple text,
                // or adapt if it was already using a template.
                // The previous diff shows it was simple text.
                 emailService.sendSimpleMail(employeeEmail, subject, textBody);


            } else {
                logger.warn("Could not send regularization status email to employee for request ID {}: Employee email or lead details missing.", updatedRequest.getId());
            }
        } else {
            logger.info("EmailService not configured. Skipping email notification to employee for regularization request ID {}.", updatedRequest.getId());
        }

        // Notify HR Users if approved
        if ("APPROVE".equalsIgnoreCase(actionRequest.getAction())) {
            if (emailService != null && employee.getCompany() != null) {
                List<User> hrUsers = userRepository.findByCompanyIdAndRoleNameAndIsActiveTrue(employee.getCompany().getId(), "ROLE_HR");
                if (hrUsers.isEmpty()) {
                    logger.warn("No active HR users found in company {} to notify for regularization approval (Req ID: {}).", employee.getCompany().getName(), updatedRequest.getId());
                } else {
                    User leadApprover = userRepository.findById(leadUserDetails.getId()).orElse(null); // leadUserEntity is already fetched
                    for (User hrRecipient : hrUsers) {
                        if (StringUtils.hasText(hrRecipient.getEmail())) {
                            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
                            String hrGreeting = "Dear HR Team,"; // Or "Dear " + hrRecipient.getFirstName() + ","
                            String emailSubject = String.format("Regularization Approved for %s %s - Action May Be Required",
                                                                employee.getFirstName(), employee.getLastName());
                            String bodyMessage = String.format(
                                "An attendance regularization request for employee %s %s (ID: %d) for date %s (Reason: %s) has been APPROVED by %s (%s).<br/>" +
                                "Approver Comment: %s<br/><br/>" +
                                "Please review and ensure this is updated in payroll and other relevant records if necessary.",
                                employee.getFirstName(), employee.getLastName(), employee.getId(),
                                updatedRequest.getRequestDate().toString(),
                                updatedRequest.getReasonType().toString() + (updatedRequest.getCustomReason() != null ? " - " + updatedRequest.getCustomReason() : ""),
                                leadApprover != null ? leadApprover.getFirstName() + " " + leadApprover.getLastName() : "their manager",
                                leadApprover != null ? leadApprover.getUsername() : "N/A",
                                updatedRequest.getApproverComment() != null ? updatedRequest.getApproverComment() : "N/A");

                            context.setVariable("greeting", hrGreeting);
                            context.setVariable("subject", emailSubject);
                            context.setVariable("bodyMessage", bodyMessage);
                            // Optional: Add specific variables if template is more detailed
                            // context.setVariable("employeeName", employee.getFirstName() + " " + employee.getLastName());
                            // context.setVariable("employeeId", employee.getId());
                            // context.setVariable("requestDate", updatedRequest.getRequestDate().toString());
                            // context.setVariable("reasonText", updatedRequest.getReasonType().toString() + (updatedRequest.getCustomReason() != null ? " - " + updatedRequest.getCustomReason() : ""));
                            // context.setVariable("approverName", leadApprover != null ? leadApprover.getFirstName() + " " + leadApprover.getLastName() : "their manager");
                            // context.setVariable("approverComment", updatedRequest.getApproverComment() != null ? updatedRequest.getApproverComment() : "N/A");

                            try {
                                emailService.sendHtmlMailFromTemplate(hrRecipient.getEmail(), emailSubject, "regularization-approved-hr-notification.html", context);
                                logger.info("Regularization approval notification sent to HR user {} for Req ID {}.", hrRecipient.getEmail(), updatedRequest.getId());
                            } catch (Exception e) {
                                logger.error("Failed to send regularization approval notification to HR user {} for Req ID {}: {}",
                                             hrRecipient.getEmail(), updatedRequest.getId(), e.getMessage(), e);
                            }
                        } else {
                             logger.warn("HR user ID {} has no email. Skipping regularization approval notification.", hrRecipient.getId());
                        }
                    }
                }
            } else if (employee.getCompany() == null && "APPROVE".equalsIgnoreCase(actionRequest.getAction())) {
                 logger.warn("Cannot send HR notification for regularization of employee ID {} as they are not associated with a company.", employee.getId());
            }
        }

        return mapToTeamRegularizationRequestDTO(updatedRequest);
    }

    private TeamRegularizationRequestDTO mapToTeamRegularizationRequestDTO(RegularizationRequest request) {
        User employee = request.getEmployee();
        String employeeName = (employee.getFirstName() != null ? employee.getFirstName() : "")
                             + " " + (employee.getLastName() != null ? employee.getLastName() : "");

        TeamRegularizationRequestDTO dto = new TeamRegularizationRequestDTO(
                request.getId(),
                employee.getId(),
                employeeName.trim(),
                request.getRequestDate(),
                request.getReasonType().name(),
                request.getCustomReason(),
                request.getStatus().name(),
                request.getCreatedAt(),
                null, null, null // Placeholder for attendance details
        );

        Attendance attendance = request.getAttendanceRecord();
        if (attendance != null) {
            dto.setAttendanceWorkDate(attendance.getWorkDate());
            dto.setAttendanceLoginTime(attendance.getLoginTime());
            dto.setAttendanceLogoutTime(attendance.getLogoutTime());
        }

        return dto;
    }
}
