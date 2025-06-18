package com.hrms.scheduler;

import com.hrms.core.entity.User;
import com.hrms.employee.core.entity.Task;
import com.hrms.employee.core.enums.TaskStatus;
import com.hrms.employee.core.repository.TaskRepository;
// UserRepository might not be needed if Task entity eagerly fetches assignedTo and assignedBy with email
import com.hrms.service.notification.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class TaskManagementJobService {

    private static final Logger logger = LoggerFactory.getLogger(TaskManagementJobService.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired(required = false)
    private EmailService emailService;

    // UserRepository might be needed if we need to fetch User entities fresh to ensure email is present
    // @Autowired
    // private UserRepository userRepository;

    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    // For testing: @Scheduled(fixedRate = 300000) // every 5 mins
    @Transactional
    public void autoCloseOverdueTasks() {
        logger.info("Starting scheduled job: AutoCloseOverdueTasks");
        LocalDateTime now = LocalDateTime.now();
        List<TaskStatus> openStatuses = Arrays.asList(TaskStatus.TODO, TaskStatus.IN_PROGRESS);

        List<Task> overdueTasks = taskRepository.findOverdueTasksForAutoClosure(now, openStatuses);

        if (overdueTasks.isEmpty()) {
            logger.info("No overdue tasks found for auto-closure.");
            return;
        }

        logger.info("Found {} overdue tasks to auto-close. Processing...", overdueTasks.size());

        for (Task task : overdueTasks) {
            TaskStatus oldStatus = task.getStatus();
            task.setStatus(TaskStatus.AUTO_CLOSED_DEADLINE_PASSED);
            task.setAutoClosedAt(now);
            taskRepository.save(task);
            logger.info("Task ID {}: Status changed from {} to {}. Auto-closed at {}.",
                        task.getId(), oldStatus, task.getStatus(), task.getAutoClosedAt());

            if (emailService == null) {
                logger.warn("EmailService not configured. Skipping notifications for auto-closed task ID {}.", task.getId());
                continue;
            }

            // Notify Assignee
            User assignedTo = task.getAssignedTo();
            if (assignedTo != null && StringUtils.hasText(assignedTo.getEmail())) {
                Context assignedContext = new Context();
                assignedContext.setVariable("greeting", "Dear " + assignedTo.getFirstName() + ",");
                assignedContext.setVariable("subject", "Task Auto-Closed: " + task.getTitle());
                assignedContext.setVariable("bodyMessage",
                    String.format("Your task '<strong>%s</strong>' which was due on %s has been automatically closed as the deadline passed.<br/>New Status: <strong>%s</strong>.",
                                  task.getTitle(), task.getDeadline().toLocalDate().toString(), task.getStatus().name()));
                // assignedContext.setVariable("actionUrl", "#"); // Optional: Link to task details in portal
                // assignedContext.setVariable("actionText", "View Task");

                try {
                    emailService.sendHtmlMailFromTemplate(assignedTo.getEmail(),
                                                        "Task Auto-Closed: " + task.getTitle(),
                                                        "task-auto-closed-notification.html", assignedContext);
                    logger.info("Auto-closure notification sent to assignee {} for Task ID {}.", assignedTo.getEmail(), task.getId());
                } catch (Exception e) {
                    logger.error("Error sending auto-closure notification to assignee for Task ID {}: {}", task.getId(), e.getMessage(), e);
                }
            } else {
                logger.warn("Assignee or assignee email missing for Task ID {}. Skipping assignee notification.", task.getId());
            }

            // Notify Assigner
            User assignedBy = task.getAssignedBy();
            if (assignedBy != null && StringUtils.hasText(assignedBy.getEmail()) && !Objects.equals(assignedBy.getId(), assignedTo.getId())) {
                Context assignerContext = new Context();
                assignerContext.setVariable("greeting", "Dear " + assignedBy.getFirstName() + ",");
                assignerContext.setVariable("subject", "Task Assigned by You Auto-Closed: " + task.getTitle());
                assignerContext.setVariable("bodyMessage",
                    String.format("The task '<strong>%s</strong>' (assigned to %s %s), which was due on %s, has been automatically closed as the deadline passed.<br/>New Status: <strong>%s</strong>.",
                                  task.getTitle(), assignedTo.getFirstName(), assignedTo.getLastName(), task.getDeadline().toLocalDate().toString(), task.getStatus().name()));

                try {
                    emailService.sendHtmlMailFromTemplate(assignedBy.getEmail(),
                                                        "Task Assigned by You Auto-Closed: " + task.getTitle(),
                                                        "task-auto-closed-assigner-notification.html", assignerContext);
                    logger.info("Auto-closure notification sent to assigner {} for Task ID {}.", assignedBy.getEmail(), task.getId());
                } catch (Exception e) {
                    logger.error("Error sending auto-closure notification to assigner for Task ID {}: {}", task.getId(), e.getMessage(), e);
                }
            }
        }
        logger.info("Finished scheduled job: AutoCloseOverdueTasks. Processed {} tasks.", overdueTasks.size());
    }
}
