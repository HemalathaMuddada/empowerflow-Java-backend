package com.hrms.lead.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.WorkStatusReport;
import com.hrms.employee.core.repository.WorkStatusReportRepository;
import com.hrms.employee.payload.request.WorkStatusReportRequest; // Reusing employee DTO
import com.hrms.employee.payload.response.WorkStatusReportResponse; // Reusing employee DTO
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.service.notification.EmailService; // Added
import org.slf4j.Logger; // Added
import org.slf4j.LoggerFactory; // Added
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Added
import org.thymeleaf.context.Context; // Added

import java.time.LocalDateTime;

@Service
public class LeadWorkStatusReportService {

    private static final Logger logger = LoggerFactory.getLogger(LeadWorkStatusReportService.class); // Added

    @Autowired
    private WorkStatusReportRepository workStatusReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false) // Added
    private EmailService emailService;

    @Transactional
    public WorkStatusReportResponse submitLeadReport(WorkStatusReportRequest request, UserDetailsImpl leadUserDetails) {
        User leadUser = userRepository.findById(leadUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Lead user not found: " + leadUserDetails.getUsername()));

        WorkStatusReport report = new WorkStatusReport();
        report.setEmployee(leadUser); // The lead is the employee submitting this report
        report.setReportDate(request.getReportDate());
        report.setTasksCompleted(request.getTasksCompleted());
        report.setTasksPending(request.getTasksPending());
        report.setBlockers(request.getBlockers());
        report.setSubmittedAt(LocalDateTime.now());
        // createdAt and updatedAt will be handled by AuditingEntityListener

        WorkStatusReport savedReport = workStatusReportRepository.save(report);

        // Notify Lead's Manager
        if (emailService != null) {
            User leadUserEntity = userRepository.findById(leadUserDetails.getId()).orElse(null); // Re-fetch for manager info
            if (leadUserEntity != null && leadUserEntity.getManager() != null) {
                User managerOfLead = leadUserEntity.getManager();
                // Could fetch managerOfLead fresh if needed: userRepository.findById(leadUserEntity.getManager().getId()).orElse(null);

                if (StringUtils.hasText(managerOfLead.getEmail())) {
                    String managerEmail = managerOfLead.getEmail();
                    String managerName = managerOfLead.getFirstName();
                    String leadName = leadUserEntity.getFirstName() + " " + leadUserEntity.getLastName();
                    String reportDateStr = savedReport.getReportDate().toString();

                    String emailSubject = String.format("Work Status Report Submitted by Lead: %s for %s", leadName, reportDateStr);

                    Context context = new Context();
                    context.setVariable("greeting", "Dear " + managerName + ",");
                    context.setVariable("subject", emailSubject);
                    context.setVariable("leadName", leadName);
                    context.setVariable("reportDate", reportDateStr);
                    // For display in HTML, newlines in these fields should be converted to <br> or handled by template's white-space style
                    context.setVariable("tasksCompleted", savedReport.getTasksCompleted().replace("\n", "<br/>"));
                    context.setVariable("tasksPending", savedReport.getTasksPending().replace("\n", "<br/>"));
                    context.setVariable("blockers", savedReport.getBlockers() != null ? savedReport.getBlockers().replace("\n", "<br/>") : "N/A");

                    // A more generic bodyMessage for simple templates if not using detailed context variables
                    // String bodyMessage = String.format("Lead %s has submitted their work status report for %s.%n%nTasks Completed:%n%s%n%nTasks Pending:%n%s%n%nBlockers:%n%s",
                    //    leadName, reportDateStr, savedReport.getTasksCompleted(), savedReport.getTasksPending(), savedReport.getBlockers() != null ? savedReport.getBlockers() : "N/A");
                    // context.setVariable("bodyMessage", bodyMessage.replace("\n", "<br/>"));


                    try {
                        emailService.sendHtmlMailFromTemplate(managerEmail, emailSubject,
                                                            "lead-status-report-manager-notification.html", context);
                        logger.info("Notification for Lead's status report (Lead: {}) sent to Manager {}.", leadName, managerEmail);
                    } catch (Exception e) {
                        logger.error("Failed to send Lead's status report notification to Manager {}: {}", managerEmail, e.getMessage(), e);
                    }
                } else {
                    logger.warn("Manager (ID: {}) of Lead (ID: {}) has no email address. Skipping notification.", managerOfLead.getId(), leadUserEntity.getId());
                }
            } else {
                if (leadUserEntity == null) logger.warn("Lead user (ID: {}) not found. Cannot determine manager for notification.", leadUserDetails.getId());
                else logger.warn("Lead (ID: {}) has no manager assigned. Skipping manager notification for status report.", leadUserEntity.getId());
            }
        } else {
            logger.info("EmailService not configured. Skipping manager notification for Lead's status report (Lead ID: {}).", leadUserDetails.getId());
        }

        return mapToWorkStatusReportResponse(savedReport);
    }

    private WorkStatusReportResponse mapToWorkStatusReportResponse(WorkStatusReport report) {
        // This mapping is identical to the one potentially in EmployeeWorkStatusReportService
        // If that service exists and has this public/protected, could reuse. For now, defining it here.
        return new WorkStatusReportResponse(
                report.getId(),
                report.getEmployee().getId(),
                report.getReportDate(),
                report.getTasksCompleted(),
                report.getTasksPending(),
                report.getBlockers(),
                report.getSubmittedAt()
        );
    }
}
