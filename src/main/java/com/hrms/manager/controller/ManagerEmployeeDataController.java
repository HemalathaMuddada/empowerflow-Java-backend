package com.hrms.manager.controller;

import com.hrms.employee.payload.response.EmployeeProfileResponse; // Reusing
import com.hrms.manager.service.ManagerEmployeeDataService;
import com.hrms.manager.service.ResourceNotFoundException; // Assuming defined in ManagerEmployeeDataService
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/manager/employee-data")
@PreAuthorize("hasRole('ROLE_MANAGER')")
public class ManagerEmployeeDataController {

    @Autowired
    private ManagerEmployeeDataService managerEmployeeDataService;

    @GetMapping("/{employeeId}/profile")
    public ResponseEntity<EmployeeProfileResponse> viewEmployeeProfile(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            EmployeeProfileResponse employeeProfile = managerEmployeeDataService.getEmployeeProfileForManager(employeeId, managerUser);
            return ResponseEntity.ok(employeeProfile);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) { // For manager not in a company
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex for server-side details
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while fetching employee profile.", ex);
        }
    }

    @GetMapping("/{employeeId}/leaves")
    public ResponseEntity<com.hrms.employee.payload.response.LeaveHistoryResponse> viewEmployeeLeaveHistory(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            com.hrms.employee.payload.response.LeaveHistoryResponse leaveHistory =
                managerEmployeeDataService.getEmployeeLeaveHistoryForManager(employeeId, managerUser);
            return ResponseEntity.ok(leaveHistory);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while fetching leave history.", ex);
        }
    }

    @GetMapping("/{employeeId}/attendance")
    public ResponseEntity<com.hrms.employee.payload.response.AttendanceSummaryResponse> viewEmployeeAttendanceRecords(
            @PathVariable Long employeeId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            com.hrms.employee.payload.response.AttendanceSummaryResponse attendanceSummary =
                managerEmployeeDataService.getEmployeeAttendanceForManager(employeeId, startDate, endDate, managerUser);
            return ResponseEntity.ok(attendanceSummary);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), ex);
        } catch (com.hrms.hr.service.BadRequestException ex) { // Assuming BadRequestException from hr.service
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while fetching attendance records.", ex);
        }
    }

    @GetMapping("/{employeeId}/tasks")
    public ResponseEntity<com.hrms.employee.payload.response.TaskListResponse> viewEmployeeTasks(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            com.hrms.employee.payload.response.TaskListResponse taskList =
                managerEmployeeDataService.getEmployeeTasksForManager(employeeId, status, managerUser);
            return ResponseEntity.ok(taskList);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) { // For manager not in a company
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) { // For invalid status string
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while fetching employee tasks.", ex);
        }
    }

    @GetMapping("/company-roster")
    public ResponseEntity<org.springframework.data.domain.Page<EmployeeProfileResponse>> getCompanyRoster(
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName,asc") String[] sort,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {

        try {
            List<org.springframework.data.domain.Sort.Order> orders = new java.util.ArrayList<>();
            if (sort[0].contains(",")) {
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    if (_sort.length == 2) orders.add(new org.springframework.data.domain.Sort.Order(org.springframework.data.domain.Sort.Direction.fromString(_sort[1]), _sort[0]));
                    else orders.add(new org.springframework.data.domain.Sort.Order(org.springframework.data.domain.Sort.Direction.ASC, "lastName")); // Default
                }
            } else {
                 if (sort.length == 2) orders.add(new org.springframework.data.domain.Sort.Order(org.springframework.data.domain.Sort.Direction.fromString(sort[1]), sort[0]));
                 else orders.add(new org.springframework.data.domain.Sort.Order(org.springframework.data.domain.Sort.Direction.ASC, "lastName")); // Default
            }
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(orders));

            org.springframework.data.domain.Page<EmployeeProfileResponse> employeePage =
                managerEmployeeDataService.getCompanyEmployeesForManager(managerUser, designation, isActive, pageable);

            return ResponseEntity.ok(employeePage);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while fetching company roster.", ex);
        }
    }
}
