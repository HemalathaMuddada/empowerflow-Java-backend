package com.hrms.lead.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.LeaveRequest;
import com.hrms.employee.core.enums.LeaveStatus;
import com.hrms.employee.core.repository.LeaveRequestRepository;
import com.hrms.lead.payload.response.PendingTeamLeavesResponse;
import com.hrms.employee.payload.response.LeaveRequestDetailsDTO;
import com.hrms.lead.payload.request.LeaveActionRequest;
import com.hrms.lead.payload.response.TeamLeaveRequestDTO;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.service.notification.EmailService; // Added EmailService
import org.slf4j.Logger; // Added Logger
import org.slf4j.LoggerFactory; // Added Logger
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LeadLeaveService {

    private static final Logger logger = LoggerFactory.getLogger(LeadLeaveService.class); // Added Logger

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired(required = false) // Make EmailService optional for environments where mail isn't configured
    private EmailService emailService;

    // Consider a shared mapper or utility if this DTO is widely used across services
    private LeaveRequestDetailsDTO mapToEmployeeLeaveRequestDetailsDTO(LeaveRequest leaveRequest) {
        return new LeaveRequestDetailsDTO(
                leaveRequest.getId(),
                leaveRequest.getLeaveType().name(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getReason(),
                leaveRequest.getStatus().name(),
                leaveRequest.getManagerComment(),
                leaveRequest.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PendingTeamLeavesResponse getPendingLeaveRequestsForMyTeam(UserDetailsImpl leadUserDetails) {
        Long leadUserId = leadUserDetails.getId();
        List<User> reportees = userRepository.findByManagerId(leadUserId);

        if (reportees.isEmpty()) {
            return new PendingTeamLeavesResponse(Collections.emptyList());
        }

        // List<Long> reporteeIds = reportees.stream().map(User::getId).collect(Collectors.toList());
        // Using List<User> directly in repository method is often cleaner if supported well by JPA provider for 'IN' clauses.
        List<LeaveRequest> pendingLeaveRequests = leaveRequestRepository.findByEmployeeInAndStatus(reportees, LeaveStatus.PENDING);

        List<TeamLeaveRequestDTO> dtoList = pendingLeaveRequests.stream()
                .map(this::mapToTeamLeaveRequestDTO)
                .collect(Collectors.toList());

        return new PendingTeamLeavesResponse(dtoList);
    }

    @Transactional
    public LeaveRequestDetailsDTO approveOrRejectLeave(Long leaveRequestId, LeaveActionRequest actionRequest, UserDetailsImpl leadUserDetails) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found with id: " + leaveRequestId)); // Or ResourceNotFoundException

        User employee = leaveRequest.getEmployee();
        if (employee.getManager() == null || !Objects.equals(employee.getManager().getId(), leadUserDetails.getId())) {
            throw new AccessDeniedException("You are not authorized to action this leave request.");
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave request is not in PENDING state. Current state: " + leaveRequest.getStatus());
        }

        String action = actionRequest.getAction().toUpperCase();
        switch (action) {
            case "APPROVE":
                leaveRequest.setStatus(LeaveStatus.APPROVED);
                break;
            case "REJECT":
                leaveRequest.setStatus(LeaveStatus.REJECTED);
                break;
            default:
                // This case should ideally not be reached if @Pattern validation works,
                // but as a safeguard:
                throw new IllegalArgumentException("Invalid action specified: " + actionRequest.getAction());
        }

        leaveRequest.setManagerComment(actionRequest.getManagerComment());
        LeaveRequest updatedLeaveRequest = leaveRequestRepository.save(leaveRequest);

        // Send email notification
        if (emailService != null) {
            User employeeForEmail = userRepository.findById(employee.getId())
                .orElse(null); // Fetch fresh user data for email
            User leadForEmail = userRepository.findById(leadUserDetails.getId())
                .orElse(null); // Fetch fresh lead data for name

            if (employeeForEmail != null && employeeForEmail.getEmail() != null && leadForEmail != null) {
                String employeeEmail = employeeForEmail.getEmail();
                String employeeName = employeeForEmail.getFirstName();
                String leadName = leadForEmail.getFirstName() + " " + leadForEmail.getLastName();
                String leaveStatusString = updatedLeaveRequest.getStatus().name();
                String subject = "Leave Request " + leaveStatusString;

                String textBody = String.format(
                    "Dear %s,\n\n" +
                    "Your leave request for the period %s to %s (Reason: %s) has been %s by %s.\n" +
                    "Manager Comment: %s\n\n" +
                    "You can view the details in the employee portal.\n\n" +
                    "Regards,\nHRMS Notification System",
                    employeeName,
                    updatedLeaveRequest.getStartDate().toString(),
                    updatedLeaveRequest.getEndDate().toString(),
                    updatedLeaveRequest.getReason(),
                    leaveStatusString.toLowerCase(),
                    leadName,
                    updatedLeaveRequest.getManagerComment() != null ? updatedLeaveRequest.getManagerComment() : "N/A"
                );
                emailService.sendSimpleMail(employeeEmail, subject, textBody);
            } else {
                logger.warn("Could not send leave status email for request ID {}: Employee email or lead details missing.", updatedLeaveRequest.getId());
            }
        } else {
            logger.info("EmailService not configured. Skipping email notification for leave request ID {}.", updatedLeaveRequest.getId());
        }

        return mapToEmployeeLeaveRequestDetailsDTO(updatedLeaveRequest);
    }

    private TeamLeaveRequestDTO mapToTeamLeaveRequestDTO(LeaveRequest leaveRequest) {
        User employee = leaveRequest.getEmployee();
        String employeeName = (employee.getFirstName() != null ? employee.getFirstName() : "")
                             + " " + (employee.getLastName() != null ? employee.getLastName() : "");

        return new TeamLeaveRequestDTO(
                leaveRequest.getId(),
                employee.getId(),
                employeeName.trim(),
                leaveRequest.getLeaveType().name(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getReason(),
                leaveRequest.getCreatedAt()
        );
    }
}
