package com.hrms.lead.controller;

import com.hrms.employee.payload.response.TaskDetailsDTO; // Reusing
import com.hrms.lead.payload.request.LeadCreateTaskRequest;
import com.hrms.lead.service.LeadTaskService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/lead/tasks")
@PreAuthorize("hasRole('ROLE_LEAD')")
public class LeadTaskController {

    @Autowired
    private LeadTaskService leadTaskService;

    @PostMapping("/assign")
    public ResponseEntity<TaskDetailsDTO> assignNewTask(
            @Valid @RequestBody LeadCreateTaskRequest taskRequest,
            @AuthenticationPrincipal UserDetailsImpl leadUser) {
        try {
            TaskDetailsDTO createdTask = leadTaskService.createAndAssignTask(taskRequest, leadUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
        } catch (RuntimeException ex) { // Catch specific exceptions
            if (ex instanceof AccessDeniedException) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
            } else if (ex.getMessage() != null && ex.getMessage().contains("not found")) { // Basic check for resource not found
                 throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
            }
            // Default to internal server error or more specific handling
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error assigning task", ex);
        }
    }
}
