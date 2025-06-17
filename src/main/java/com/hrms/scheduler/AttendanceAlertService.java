package com.hrms.scheduler;

import com.hrms.core.entity.User;
import com.hrms.employee.core.entity.Attendance;
import com.hrms.employee.core.repository.AttendanceRepository;
import com.hrms.employee.core.repository.HolidayRepository;
import com.hrms.service.config.SystemConfigValueProviderService; // Added
import com.hrms.service.notification.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttendanceAlertService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceAlertService.class);

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired // Added
    private SystemConfigValueProviderService configValueProvider;

    // private static final double MINIMUM_HOURS_THRESHOLD = 8.0; // Replaced by config

    // Runs at 2:00 AM server time, Monday to Friday
    @Scheduled(cron = "0 0 2 * * MON-FRI")
    // For testing: @Scheduled(fixedRate = 600000) // every 10 mins
    @Transactional // Ensure operations within are part of a transaction, especially updates
    public void checkAndNotifyForUnderworkedHours() {
        logger.info("Starting scheduled task: CheckAndNotifyForUnderworkedHours");
        double minHours = configValueProvider.getDoubleValue("MINIMUM_WORK_HOURS_PER_DAY", 8.0);


        LocalDate dateToCheck = determinePreviousWorkingDay(LocalDate.now());
        if (dateToCheck == null) {
            logger.info("Skipping underwork check; today is not a scheduled day for this check (e.g. weekend, or Monday checking Sunday).");
            return;
        }

        logger.info("Checking for underworked hours for date: {} against threshold: {}", dateToCheck, minHours);

        // Check if dateToCheck is a global holiday.
        // Assuming existsByDateAndIsGlobalTrue() or similar. For now, using findByDate and checking if any isGlobal.
        // This logic might need refinement if holidays are company-specific and users are in different companies.
        // For a system-wide job, checking against global holidays is a safe start.
        boolean isHoliday = holidayRepository.findByDate(dateToCheck).stream().anyMatch(h -> h.isGlobal());
        if (isHoliday) {
            logger.info("Date {} is a global holiday. Skipping underworked hours check.", dateToCheck);
            return;
        }

        // Fetch attendance records for underworked hours, not regularized, and alert not yet sent.
        List<Attendance> underworkedRecords = attendanceRepository.findUnderworkedAttendanceForAlert(
                dateToCheck, minHours);

        if (underworkedRecords.isEmpty()) {
            logger.info("No underworked attendance records found for {} that need alerting against threshold {}.", dateToCheck, minHours);
            return;
        }

        logger.info("Found {} underworked records for {}. Processing notifications...", underworkedRecords.size(), dateToCheck);

        for (Attendance record : underworkedRecords) {
            User employee = record.getEmployee();
            if (employee == null) {
                logger.warn("Attendance record ID {} has no associated employee. Skipping.", record.getId());
                continue;
            }

            if (emailService != null && StringUtils.hasText(employee.getEmail())) {
                try {
                    String subject = "Alert: Work Hours Below Threshold for " + dateToCheck.toString();
                    String body = String.format(
                        "Dear %s,\n\n" +
                        "Your recorded work hours for %s were %.2f, which is below the expected %.1f hours.<br/>" + // Using <br/> for HTML
                        "Please ensure your attendance is accurately recorded or apply for regularization if needed through the employee portal.",
                        employee.getFirstName(),
                        dateToCheck.toString(),
                        record.getTotalHours(),
                        minHours // Use fetched config value
                    );
                     org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
                    context.setVariable("greeting", "Dear " + employee.getFirstName() + ",");
                    context.setVariable("subject", subject);
                    context.setVariable("bodyMessage", body); // body is already formatted HTML-like string

                    emailService.sendHtmlMailFromTemplate(employee.getEmail(), subject, "generic-notification.html", context);

                    // Mark as alerted
                    record.setUnderworkAlertSentAt(LocalDateTime.now());
                    attendanceRepository.save(record);
                    logger.info("Underwork alert sent to employee ID {} for attendance ID {}.", employee.getId(), record.getId());

                } catch (Exception e) {
                    logger.error("Failed to send underwork alert email to employee ID {} for attendance ID {}: {}",
                                 employee.getId(), record.getId(), e.getMessage(), e);
                }
            } else {
                if(emailService == null) logger.warn("EmailService not configured. Skipping alert for employee ID {} (Att. ID {}).", employee.getId(), record.getId());
                else logger.warn("Employee ID {} (Att. ID {}) has no email address. Skipping underwork alert.", employee.getId(), record.getId());
            }
        }
        logger.info("Finished scheduled task: CheckAndNotifyForUnderworkedHours");
    }

    // Runs at 8:15 AM server time, Monday to Friday
    @Scheduled(cron = "0 15 8 * * MON-FRI")
    @Transactional
    public void checkForMissedLogouts() {
        logger.info("Starting scheduled task: CheckForMissedLogouts");
        LocalDate dateToCheck = determinePreviousWorkingDay(LocalDate.now());

        if (dateToCheck == null) {
            logger.info("Skipping missed logout check; today is not a scheduled day for this check.");
            return;
        }
        logger.info("Checking for missed logouts for date: {}", dateToCheck);

        boolean isHoliday = holidayRepository.findByDate(dateToCheck).stream().anyMatch(h -> h.isGlobal());
        if (isHoliday) {
            logger.info("Date {} is a global holiday. Skipping missed logout check.", dateToCheck);
            return;
        }

        List<Attendance> missedLogoutRecords = attendanceRepository.findAttendanceWithMissedLogoutForAlert(dateToCheck);

        if (missedLogoutRecords.isEmpty()) {
            logger.info("No attendance records with missed logouts found for {} that need alerting.", dateToCheck);
            return;
        }

        logger.info("Found {} records with missed logouts for {}. Processing notifications...", missedLogoutRecords.size(), dateToCheck);

        for (Attendance record : missedLogoutRecords) {
            User employee = record.getEmployee();
            if (employee == null) {
                logger.warn("Attendance record ID {} (for missed logout) has no associated employee. Skipping.", record.getId());
                continue;
            }

            if (emailService != null && StringUtils.hasText(employee.getEmail())) {
                try {
                    String subject = "Alert: Missed Logout on " + dateToCheck.toString();

                    org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
                    context.setVariable("greeting", "Dear " + employee.getFirstName() + ",");
                    context.setVariable("subject", subject);
                    context.setVariable("bodyMessage",
                        String.format("We noticed you logged in on %s at %s but there's no corresponding logout time recorded.<br/>" +
                                      "Please update your attendance record for this day or apply for regularization if necessary via the employee portal.",
                                      dateToCheck.toString(),
                                      record.getLoginTime().toLocalTime().toString()));
                    // context.setVariable("actionUrl", "#"); // Optional: Link to attendance page
                    // context.setVariable("actionText", "Update Attendance");

                    emailService.sendHtmlMailFromTemplate(employee.getEmail(), subject, "missed-logout-alert.html", context);

                    record.setMissedLogoutAlertSentAt(LocalDateTime.now());
                    attendanceRepository.save(record);
                    logger.info("Missed logout alert sent to employee ID {} for attendance ID {}.", employee.getId(), record.getId());

                } catch (Exception e) {
                    logger.error("Failed to send missed logout alert email to employee ID {} for attendance ID {}: {}",
                                 employee.getId(), record.getId(), e.getMessage(), e);
                }
            } else {
                if(emailService == null) logger.warn("EmailService not configured. Skipping missed logout alert for employee ID {} (Att. ID {}).", employee.getId(), record.getId());
                else logger.warn("Employee ID {} (Att. ID {}) has no email address. Skipping missed logout alert.", employee.getId(), record.getId());
            }
        }
        logger.info("Finished scheduled task: CheckForMissedLogouts. Processed {} records.", missedLogoutRecords.size());
    }

    private static final double MINIMUM_WORK_HOURS_FOR_FULL_DAY = 8.0;

    // Runs at 8:20 AM server time, Monday to Friday
    @Scheduled(cron = "0 20 8 * * MON-FRI")
    @Transactional
    public void checkForEarlyLogouts() {
        logger.info("Starting scheduled task: CheckForEarlyLogouts");
        LocalDate dateToCheck = determinePreviousWorkingDay(LocalDate.now());

        if (dateToCheck == null) {
            logger.info("Skipping early logout check; today is not a scheduled day for this check.");
            return;
        }
        logger.info("Checking for early logouts for date: {}", dateToCheck);

        boolean isHoliday = holidayRepository.findByDate(dateToCheck).stream().anyMatch(h -> h.isGlobal());
        if (isHoliday) {
            logger.info("Date {} is a global holiday. Skipping early logout check.", dateToCheck);
            return;
        }

        List<Attendance> earlyLogoutRecords = attendanceRepository.findEarlyLogoutsForAlert(dateToCheck, MINIMUM_WORK_HOURS_FOR_FULL_DAY);

        if (earlyLogoutRecords.isEmpty()) {
            logger.info("No early logout records found for {} that need alerting.", dateToCheck);
            return;
        }

        logger.info("Found {} records with early logouts for {}. Processing notifications...", earlyLogoutRecords.size(), dateToCheck);

        for (Attendance record : earlyLogoutRecords) {
            User employee = record.getEmployee();
            if (employee == null) {
                logger.warn("Attendance record ID {} (for early logout) has no associated employee. Skipping.", record.getId());
                continue;
            }

            if (emailService != null && StringUtils.hasText(employee.getEmail())) {
                try {
                    String subject = "Alert: Early Logout Detected for " + dateToCheck.toString();

                    org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
                    context.setVariable("greeting", "Dear " + employee.getFirstName() + ",");
                    context.setVariable("subject", subject);
                    context.setVariable("bodyMessage",
                        String.format("Your attendance record for %s shows an early logout. Your total recorded work hours were %.2f, which is less than the expected %.1f hours.<br/>" +
                                      "Please review your attendance or apply for regularization if this is incorrect.",
                                      dateToCheck.toString(),
                                      record.getTotalHours(),
                                      MINIMUM_WORK_HOURS_FOR_FULL_DAY));
                    // context.setVariable("actionUrl", "#"); // Optional
                    // context.setVariable("actionText", "Review Attendance");

                    emailService.sendHtmlMailFromTemplate(employee.getEmail(), subject, "early-logout-alert.html", context);

                    record.setEarlyLogoutAlertSentAt(LocalDateTime.now());
                    attendanceRepository.save(record);
                    logger.info("Early logout alert sent to employee ID {} for attendance ID {}.", employee.getId(), record.getId());

                } catch (Exception e) {
                    logger.error("Failed to send early logout alert email to employee ID {} for attendance ID {}: {}",
                                 employee.getId(), record.getId(), e.getMessage(), e);
                }
            } else {
                if(emailService == null) logger.warn("EmailService not configured. Skipping early logout alert for employee ID {} (Att. ID {}).", employee.getId(), record.getId());
                else logger.warn("Employee ID {} (Att. ID {}) has no email address. Skipping early logout alert.", employee.getId(), record.getId());
            }
        }
        logger.info("Finished scheduled task: CheckForEarlyLogouts. Processed {} records.", earlyLogoutRecords.size());
    }

    // private static final LocalTime LATE_LOGIN_THRESHOLD = LocalTime.of(9, 30); // Replaced by config

    // Runs at 8:25 AM server time, Monday to Friday
    @Scheduled(cron = "0 25 8 * * MON-FRI")
    @Transactional
    public void checkForLateLogins() {
        logger.info("Starting scheduled task: CheckForLateLogins");
        LocalTime lateThreshold = configValueProvider.getLocalTimeValue("LATE_LOGIN_THRESHOLD_TIME", LocalTime.of(9, 30));

        LocalDate dateToCheck = determinePreviousWorkingDay(LocalDate.now());

        if (dateToCheck == null) {
            logger.info("Skipping late login check; today is not a scheduled day for this check.");
            return;
        }
        logger.info("Checking for late logins for date: {} against threshold: {}", dateToCheck, lateThreshold);

        boolean isHoliday = holidayRepository.findByDate(dateToCheck).stream().anyMatch(h -> h.isGlobal());
        if (isHoliday) {
            logger.info("Date {} is a global holiday. Skipping late login check.", dateToCheck);
            return;
        }

        List<Attendance> lateLoginRecords = attendanceRepository.findLateLoginsForAlert(dateToCheck, lateThreshold);

        if (lateLoginRecords.isEmpty()) {
            logger.info("No late login records found for {} that need alerting against threshold {}.", dateToCheck, lateThreshold);
            return;
        }

        logger.info("Found {} records with late logins for {}. Processing notifications...", lateLoginRecords.size(), dateToCheck);

        for (Attendance record : lateLoginRecords) {
            User employee = record.getEmployee();
            if (employee == null) {
                logger.warn("Attendance record ID {} (for late login) has no associated employee. Skipping.", record.getId());
                continue;
            }

            if (emailService != null && StringUtils.hasText(employee.getEmail())) {
                try {
                    String subject = "Alert: Late Login Detected for " + dateToCheck.toString();

                    org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
                    context.setVariable("greeting", "Dear " + employee.getFirstName() + ",");
                    context.setVariable("subject", subject);
                    context.setVariable("bodyMessage",
                        String.format("Your attendance record for %s shows a late login at %s. The expected login time is by %s.<br/>" +
                                      "Please review your attendance or apply for regularization if this is incorrect.",
                                      dateToCheck.toString(),
                                      record.getLoginTime().toLocalTime().toString(),
                                      lateThreshold.toString())); // Use fetched config value
                    // context.setVariable("actionUrl", "#"); // Optional
                    // context.setVariable("actionText", "Review Attendance");

                    emailService.sendHtmlMailFromTemplate(employee.getEmail(), subject, "late-login-alert.html", context);

                    record.setLateLoginAlertSentAt(LocalDateTime.now());
                    attendanceRepository.save(record);
                    logger.info("Late login alert sent to employee ID {} for attendance ID {}.", employee.getId(), record.getId());

                } catch (Exception e) {
                    logger.error("Failed to send late login alert email to employee ID {} for attendance ID {}: {}",
                                 employee.getId(), record.getId(), e.getMessage(), e);
                }
            } else {
                if(emailService == null) logger.warn("EmailService not configured. Skipping late login alert for employee ID {} (Att. ID {}).", employee.getId(), record.getId());
                else logger.warn("Employee ID {} (Att. ID {}) has no email address. Skipping late login alert.", employee.getId(), record.getId());
            }
        }
        logger.info("Finished scheduled task: CheckForLateLogins. Processed {} records.", lateLoginRecords.size());
    }

    // Runs at 8:30 AM server time, Monday to Friday
    @Scheduled(cron = "0 30 8 * * MON-FRI")
    @Transactional
    public void checkForUnrecordedLeave() {
        logger.info("Starting scheduled task: CheckForUnrecordedLeave");
        LocalDate dateToCheck = determinePreviousWorkingDay(LocalDate.now());

        if (dateToCheck == null) {
            logger.info("Skipping unrecorded leave check; today is not a scheduled day for this check.");
            return;
        }
        logger.info("Checking for unrecorded leave for date: {}", dateToCheck);

        if (holidayRepository.existsByDateAndIsGlobalTrue(dateToCheck)) {
            logger.info("Date {} is a global holiday. Skipping unrecorded leave check for all users.", dateToCheck);
            return;
        }

        List<User> activeEmployees = userRepository.findByIsActiveTrue();
        if (activeEmployees.isEmpty()) {
            logger.info("No active employees found. Skipping unrecorded leave check.");
            return;
        }

        logger.info("Checking {} active employees for unrecorded leave on {}.", activeEmployees.size(), dateToCheck);

        for (User employee : activeEmployees) {
            // Skip if it was a company-specific holiday for this employee
            if (employee.getCompany() != null && holidayRepository.existsByCompanyIdAndDate(employee.getCompany().getId(), dateToCheck)) {
                logger.info("Employee ID {} was on company holiday on {}. Skipping unrecorded leave check.", employee.getId(), dateToCheck);
                continue;
            }

            boolean hasAttendance = attendanceRepository.existsByEmployeeAndWorkDate(employee, dateToCheck);
            if (hasAttendance) {
                continue; // Employee has an attendance record, so not an unrecorded day
            }

            boolean hasApprovedLeave = leaveRequestRepository.hasApprovedLeaveForDate(employee, dateToCheck);
            if (hasApprovedLeave) {
                continue; // Employee has approved leave, so not an unrecorded day
            }

            // If we reach here, it's an unrecorded leave instance
            logger.info("Unrecorded leave detected for employee ID {} on date {}.", employee.getId(), dateToCheck);

            if (emailService != null && StringUtils.hasText(employee.getEmail())) {
                try {
                    String subject = "Alert: Unrecorded Workday/Leave for " + dateToCheck.toString();

                    org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
                    context.setVariable("greeting", "Dear " + employee.getFirstName() + ",");
                    context.setVariable("subject", subject);
                    context.setVariable("bodyMessage",
                        String.format("Our records show no attendance or approved leave registered for you on %s, which was an expected workday.<br/>" +
                                      "Please ensure your attendance is accurately recorded, apply for leave if applicable, " +
                                      "or contact your manager/HR if you believe this is an error.",
                                      dateToCheck.toString()));
                    // context.setVariable("actionUrl", "#"); // Optional
                    // context.setVariable("actionText", "Update Records");

                    emailService.sendHtmlMailFromTemplate(employee.getEmail(), subject, "unrecorded-leave-alert.html", context);
                    logger.info("Unrecorded leave alert sent to employee ID {}.", employee.getId());
                    // Note: We are not creating an "alert sent" timestamp for this type of alert on any entity currently.
                    // This means the employee might get alerted again if the condition persists next time the job runs for the same dateToCheck (unlikely with daily runs).
                } catch (Exception e) {
                    logger.error("Failed to send unrecorded leave alert email to employee ID {}: {}",
                                 employee.getId(), e.getMessage(), e);
                }
            } else {
                if(emailService == null) logger.warn("EmailService not configured. Skipping unrecorded leave alert for employee ID {}.", employee.getId());
                else logger.warn("Employee ID {} has no email address. Skipping unrecorded leave alert.", employee.getId());
            }
        }
        logger.info("Finished scheduled task: CheckForUnrecordedLeave.");
    }


    private LocalDate determinePreviousWorkingDay(LocalDate today) {
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return null; // Job shouldn't run or do anything on weekends based on cron, but defensive.
        }
        if (dayOfWeek == DayOfWeek.MONDAY) {
            return today.minusDays(3); // Check Friday's work
        }
        return today.minusDays(1); // Check previous day's work
    }
}
