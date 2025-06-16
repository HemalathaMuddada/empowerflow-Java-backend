package com.hrms.lead.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Task;
import com.hrms.employee.core.enums.TaskStatus;
import com.hrms.employee.core.repository.TaskRepository;
import com.hrms.employee.payload.response.TaskDetailsDTO; // Reusing
import com.hrms.lead.payload.request.LeadCreateTaskRequest;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // For lead user
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class LeadTaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public TaskDetailsDTO createAndAssignTask(LeadCreateTaskRequest request, UserDetailsImpl leadUserDetails) {
        User leadUser = userRepository.findById(leadUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Lead user not found: " + leadUserDetails.getUsername()));

        User assignedToUser = userRepository.findById(request.getAssignedToEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee to assign task not found with ID: " + request.getAssignedToEmployeeId())); // Or ResourceNotFoundException

        // Verification: Check if assignedToUser reports to leadUser
        if (assignedToUser.getManager() == null || !Objects.equals(assignedToUser.getManager().getId(), leadUser.getId())) {
            throw new AccessDeniedException("You can only assign tasks to your direct reportees. Employee "
                    + assignedToUser.getUsername() + " does not report to you.");
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setPriority(request.getPriority());
        task.setAssignedBy(leadUser);
        task.setAssignedTo(assignedToUser);
        task.setStatus(TaskStatus.TODO); // Initial status
        // createdAt and updatedAt will be set by AuditingEntityListener
        // completedAt will be null initially

        Task savedTask = taskRepository.save(task);
        return mapToTaskDetailsDTO(savedTask);
    }

    // This mapper is identical to the one in employee.service.TaskService
    // Consider moving to a common utility or mapper class if more reuse occurs.
    private TaskDetailsDTO mapToTaskDetailsDTO(Task task) {
        User assignedBy = task.getAssignedBy();
        String assignedByUsername = (assignedBy != null) ? assignedBy.getUsername() : "N/A";

        return new TaskDetailsDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                assignedByUsername,
                task.getDeadline(),
                task.getStatus().name(),
                task.getPriority(),
                task.getRelatedProject(), // This field was in Task entity, ensure it's handled or nullable
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt()
        );
    }
}
