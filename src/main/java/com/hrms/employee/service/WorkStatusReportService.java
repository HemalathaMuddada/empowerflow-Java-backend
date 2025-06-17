package com.hrms.employee.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.WorkStatusReport;
import com.hrms.employee.core.repository.WorkStatusReportRepository;
import com.hrms.employee.payload.request.WorkStatusReportRequest;
import com.hrms.employee.payload.response.WorkStatusReportResponse;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.service.notification.EmailService; // Added EmailService
import org.slf4j.Logger; // Added Logger
import org.slf4j.LoggerFactory; // Added Logger
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // For checking manager's email

import java.time.LocalDateTime;

@Service
public class WorkStatusReportService {

    private static final Logger logger = LoggerFactory.getLogger(WorkStatusReportService.class); // Added Logger

    @Autowired
    private WorkStatusReportRepository workStatusReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false) // Make EmailService optional
    private EmailService emailService;

    @Transactional
    public WorkStatusReportResponse submitReport(WorkStatusReportRequest request, UserDetailsImpl currentUserDetails) {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        WorkStatusReport report = new WorkStatusReport();
        report.setEmployee(user);
        report.setReportDate(request.getReportDate());
        report.setTasksCompleted(request.getTasksCompleted());
        report.setTasksPending(request.getTasksPending());
        report.setBlockers(request.getBlockers());
        report.setSubmittedAt(LocalDateTime.now()); // Set by the system on submission
        // createdAt and updatedAt will be handled by AuditingEntityListener

        WorkStatusReport savedReport = workStatusReportRepository.save(report);

        // Send email notification to manager
        if (emailService != null) {
            // Fetch full employee entity to get manager details
            User employee = userRepository.findById(currentUserDetails.getId()).orElse(null);
            if (employee != null && employee.getManager() != null) {
                User manager = employee.getManager(); // Assuming manager is eagerly fetched or session is active
                // If manager might be lazily loaded and session closed, fetch it:
                // User manager = userRepository.findById(employee.getManager().getId()).orElse(null);

                if (manager != null && StringUtils.hasText(manager.getEmail())) {
                    String managerEmail = manager.getEmail();
                    String managerName = manager.getFirstName();
                    String employeeName = employee.getFirstName() + " " + employee.getLastName();
                    String reportDateStr = savedReport.getReportDate().toString();

                    String subject = String.format("Work Status Report Submitted by %s for %s", employeeName, reportDateStr);
                    String textBody = String.format(
                        "Dear %s,\n\n" +
                        "%s has submitted their work status report for %s.\n\n" +
                        "Tasks Completed:\n%s\n\n" +
                        "Tasks Pending:\n%s\n\n" +
                        "Blockers:\n%s\n\n" +
                        "You can view more details in the portal if necessary.\n\n" +
                        "Regards,\nHRMS Notification System",
                        managerName,
                        employeeName, reportDateStr,
                        savedReport.getTasksCompleted(),
                        savedReport.getTasksPending(),
                        savedReport.getBlockers() != null ? savedReport.getBlockers() : "N/A"
                    );
                    emailService.sendSimpleMail(managerEmail, subject, textBody);
                } else {
                    logger.warn("Manager or manager's email is missing for employee ID {}. Cannot send work status report notification.", employee.getId());
                }
            } else {
                 logger.warn("Employee (ID: {}) not found or has no manager. Skipping work status report notification to manager.", currentUserDetails.getId());
            }
        } else {
            logger.info("EmailService not configured. Skipping manager notification for work status report by employee ID {}.", currentUserDetails.getId());
        }

        return mapToWorkStatusReportResponse(savedReport);
    }

    private WorkStatusReportResponse mapToWorkStatusReportResponse(WorkStatusReport report) {
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
