package com.hrms.employee.service;

import com.hrms.core.entity.User; // Required for assignedBy.getUsername()
import com.hrms.employee.core.entity.Task;
import com.hrms.employee.core.enums.TaskStatus;
import com.hrms.employee.core.repository.TaskRepository;
// UserRepository might not be needed if UserDetailsImpl has all required info from User entity for assignedTo
// For assignedBy, we need to fetch the User entity if Task stores only assignedById.
// Assuming Task entity has a User object for assignedBy.
import com.hrms.employee.payload.request.TaskUpdateRequest;
import com.hrms.employee.payload.response.TaskDetailsDTO;
import com.hrms.employee.payload.response.TaskListResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    // Assuming UserDetailsImpl has employee's ID.
    // UserRepository might be needed if we need to fetch the User entity itself for 'assignedTo'
    // or if 'assignedBy' in Task entity is just an ID and we need to fetch its details.
    // For this implementation, Task entity is assumed to have User objects for assignedTo and assignedBy.

    @Transactional(readOnly = true)
    public TaskListResponse getMyTasks(UserDetailsImpl currentUserDetails, String filterByStatusString) {
        Long currentUserId = currentUserDetails.getId();
        TaskStatus filterByStatus = null;

        if (StringUtils.hasText(filterByStatusString)) {
            try {
                filterByStatus = TaskStatus.valueOf(filterByStatusString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid task status filter: " + filterByStatusString);
            }
        }

        List<Task> tasks;
        if (filterByStatus != null) {
            tasks = taskRepository.findByAssignedToIdAndStatusOrderByDeadlineAscPriorityDesc(currentUserId, filterByStatus);
        } else {
            tasks = taskRepository.findByAssignedToIdOrderByDeadlineAscPriorityDesc(currentUserId);
        }

        List<TaskDetailsDTO> dtoList = tasks.stream()
                .map(this::mapToTaskDetailsDTO)
                .collect(Collectors.toList());

        return new TaskListResponse(dtoList);
    }

    @Transactional
    public TaskDetailsDTO updateMyTaskStatus(Long taskId, TaskUpdateRequest request, UserDetailsImpl currentUserDetails) {
        Long currentUserId = currentUserDetails.getId();

        Task task = taskRepository.findByIdAndAssignedToId(taskId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Task not found or not assigned to you."));

        TaskStatus newStatus;
        try {
            newStatus = TaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid task status: " + request.getStatus());
        }

        task.setStatus(newStatus);
        if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.CLOSED_SUCCESS || newStatus == TaskStatus.CLOSED_FAILED) {
            if (task.getCompletedAt() == null) { // Set completedAt only if it's not already set
                 task.setCompletedAt(LocalDateTime.now());
            }
        } else {
            // If task is reopened, clear completedAt
            task.setCompletedAt(null);
        }

        Task updatedTask = taskRepository.save(task);
        return mapToTaskDetailsDTO(updatedTask);
    }

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
                task.getRelatedProject(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt()
        );
    }
}
