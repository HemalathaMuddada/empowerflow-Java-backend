package com.hrms.employee.controller;

import com.hrms.employee.payload.request.TaskUpdateRequest;
import com.hrms.employee.payload.response.TaskListResponse;
import com.hrms.employee.payload.response.TaskDetailsDTO;
import com.hrms.employee.service.TaskService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/tasks")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/my-tasks")
    public ResponseEntity<TaskListResponse> getMyAssignedTasks(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) String status) {
        TaskListResponse response = taskService.getMyTasks(currentUser, status);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskDetailsDTO> updateTaskStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest updateRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        TaskDetailsDTO updatedTask = taskService.updateMyTaskStatus(taskId, updateRequest, currentUser);
        return ResponseEntity.ok(updatedTask);
    }
}
