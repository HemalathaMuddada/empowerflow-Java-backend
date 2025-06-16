package com.hrms.manager.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.EmployeeProfileResponse; // Reusing
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// Local exception for this service
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

@Service
public class ManagerEmployeeDataService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public EmployeeProfileResponse getEmployeeProfileForManager(Long employeeId, UserDetailsImpl managerUserDetails) {
        User manager = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager details not found for current user with ID: " + managerUserDetails.getId()));

        User employeeToView = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        // Permission Check
        Company managerCompany = manager.getCompany();
        if (managerCompany == null) {
            throw new IllegalStateException("Manager " + manager.getUsername() + " is not associated with any company.");
        }

        Company employeeCompany = employeeToView.getCompany();
        if (employeeCompany == null || !Objects.equals(employeeCompany.getId(), managerCompany.getId())) {
            throw new AccessDeniedException("Manager can only view profiles of employees within their own company. Employee "
                                            + employeeToView.getUsername() + " is not in company " + managerCompany.getName());
        }

        // Additional check: Manager should typically only view their direct/indirect reportees,
        // or at least not other managers at same/higher level unless specifically allowed.
        // For now, same-company is the primary check as per instructions.
        // A check like `employeeToView.getManager() != null && employeeToView.getManager().getId().equals(manager.getId())`
        // would restrict to direct reportees only. Current instructions only specify "within their own company".

        return mapToEmployeeProfileResponse(employeeToView);
    }

    // This mapper should be identical/similar to ones in HREmployeeService, EmployeeProfileService etc.
    // Consider moving to a common UserMapper class or component.
    private EmployeeProfileResponse mapToEmployeeProfileResponse(User user) {
        String companyName = (user.getCompany() != null) ? user.getCompany().getName() : null;
        List<String> roleNames = user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toList());
        return new EmployeeProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getDateOfBirth(),
                companyName,
                roleNames,
                user.getDesignation(),
                user.isActive()
        );
    }

    @Autowired
    private com.hrms.employee.core.repository.LeaveRequestRepository leaveRequestRepository; // Added

    // Helper method to map LeaveRequest to LeaveRequestDetailsDTO
    // This might be duplicated from employee.service.LeaveService. Consider a common mapper.
    private com.hrms.employee.payload.response.LeaveRequestDetailsDTO mapToLeaveRequestDetailsDTO(com.hrms.employee.core.entity.LeaveRequest leaveRequest) {
        return new com.hrms.employee.payload.response.LeaveRequestDetailsDTO(
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
    public com.hrms.employee.payload.response.LeaveHistoryResponse getEmployeeLeaveHistoryForManager(
            Long employeeId, UserDetailsImpl managerUserDetails) {

        User manager = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager details not found for current user with ID: " + managerUserDetails.getId()));

        User employeeToView = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        // Permission Check
        Company managerCompany = manager.getCompany();
        if (managerCompany == null) {
            throw new IllegalStateException("Manager " + manager.getUsername() + " is not associated with any company.");
        }

        Company employeeCompany = employeeToView.getCompany();
        if (employeeCompany == null || !Objects.equals(employeeCompany.getId(), managerCompany.getId())) {
            throw new AccessDeniedException("Manager can only view leave history of employees within their own company. Employee "
                                            + employeeToView.getUsername() + " is not in company " + managerCompany.getName());
        }

        List<com.hrms.employee.core.entity.LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeOrderByCreatedAtDesc(employeeToView);

        List<com.hrms.employee.payload.response.LeaveRequestDetailsDTO> dtoList = leaveRequests.stream()
                .map(this::mapToLeaveRequestDetailsDTO)
                .collect(Collectors.toList());

        return new com.hrms.employee.payload.response.LeaveHistoryResponse(dtoList);
    }

    @Autowired
    private com.hrms.employee.core.repository.AttendanceRepository attendanceRepository; // Added

    // Helper method to map Attendance to AttendanceRecordDTO
    // This might be duplicated from employee.service.AttendanceService. Consider a common mapper.
    private com.hrms.employee.payload.response.AttendanceRecordDTO mapToAttendanceRecordDTO(com.hrms.employee.core.entity.Attendance attendance) {
        return new com.hrms.employee.payload.response.AttendanceRecordDTO(
                attendance.getId(),
                attendance.getWorkDate(),
                attendance.getLoginTime(),
                attendance.getLogoutTime(),
                attendance.getTotalHours(),
                attendance.isRegularized(),
                attendance.getNotes()
        );
    }

    @Transactional(readOnly = true)
    public com.hrms.employee.payload.response.AttendanceSummaryResponse getEmployeeAttendanceForManager(
            Long employeeId, LocalDate startDate, LocalDate endDate, UserDetailsImpl managerUserDetails) {

        if (startDate.isAfter(endDate)) {
            throw new com.hrms.hr.service.BadRequestException("Start date cannot be after end date."); // Assuming BadRequestException from hr.service
        }

        User manager = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager details not found for current user with ID: " + managerUserDetails.getId()));

        User employeeToView = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        // Permission Check
        Company managerCompany = manager.getCompany();
        if (managerCompany == null) {
            throw new IllegalStateException("Manager " + manager.getUsername() + " is not associated with any company.");
        }

        Company employeeCompany = employeeToView.getCompany();
        if (employeeCompany == null || !Objects.equals(employeeCompany.getId(), managerCompany.getId())) {
            throw new AccessDeniedException("Manager can only view attendance of employees within their own company. Employee "
                                            + employeeToView.getUsername() + " is not in company " + managerCompany.getName());
        }

        List<com.hrms.employee.core.entity.Attendance> attendanceRecords =
            attendanceRepository.findByEmployeeAndWorkDateBetweenOrderByWorkDateAsc(employeeToView, startDate, endDate);

        List<com.hrms.employee.payload.response.AttendanceRecordDTO> dtoList = attendanceRecords.stream()
                .map(this::mapToAttendanceRecordDTO)
                .collect(Collectors.toList());

        return new com.hrms.employee.payload.response.AttendanceSummaryResponse(dtoList);
    }

    @Autowired
    private com.hrms.employee.core.repository.TaskRepository taskRepository; // Added

    // Helper method to map Task to TaskDetailsDTO
    // This might be duplicated from employee.service.TaskService. Consider a common mapper.
    private com.hrms.employee.payload.response.TaskDetailsDTO mapToTaskDetailsDTO(com.hrms.employee.core.entity.Task task) {
        User assignedBy = task.getAssignedBy();
        String assignedByUsername = (assignedBy != null) ? assignedBy.getUsername() : "N/A";
        return new com.hrms.employee.payload.response.TaskDetailsDTO(
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

    @Transactional(readOnly = true)
    public com.hrms.employee.payload.response.TaskListResponse getEmployeeTasksForManager(
            Long employeeId, String filterByStatusString, UserDetailsImpl managerUserDetails) {

        User manager = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager details not found for current user with ID: " + managerUserDetails.getId()));

        User employeeToView = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        // Permission Check
        Company managerCompany = manager.getCompany();
        if (managerCompany == null) {
            throw new IllegalStateException("Manager " + manager.getUsername() + " is not associated with any company.");
        }
        Company employeeCompany = employeeToView.getCompany();
        if (employeeCompany == null || !Objects.equals(employeeCompany.getId(), managerCompany.getId())) {
            throw new AccessDeniedException("Manager can only view tasks of employees within their own company. Employee "
                                            + employeeToView.getUsername() + " is not in company " + managerCompany.getName());
        }

        com.hrms.employee.core.enums.TaskStatus statusEnum = null;
        if (org.springframework.util.StringUtils.hasText(filterByStatusString)) {
            try {
                statusEnum = com.hrms.employee.core.enums.TaskStatus.valueOf(filterByStatusString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid task status filter value: " + filterByStatusString);
            }
        }

        List<com.hrms.employee.core.entity.Task> tasks;
        if (statusEnum != null) {
            tasks = taskRepository.findByAssignedToIdAndStatusOrderByDeadlineAscPriorityDesc(employeeId, statusEnum);
        } else {
            tasks = taskRepository.findByAssignedToIdOrderByDeadlineAscPriorityDesc(employeeId);
        }

        List<com.hrms.employee.payload.response.TaskDetailsDTO> dtoList = tasks.stream()
                .map(this::mapToTaskDetailsDTO)
                .collect(Collectors.toList());

        return new com.hrms.employee.payload.response.TaskListResponse(dtoList);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<EmployeeProfileResponse> getCompanyEmployeesForManager(
            UserDetailsImpl managerUserDetails,
            String designationFilter,
            Boolean isActiveFilter,
            org.springframework.data.domain.Pageable pageable) {

        User manager = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager details not found for current user with ID: " + managerUserDetails.getId()));

        Company managerCompany = manager.getCompany();
        if (managerCompany == null) {
            throw new IllegalStateException("Manager " + manager.getUsername() + " is not associated with any company.");
        }
        Long companyId = managerCompany.getId();

        com.hrms.superadmin.specs.UserSpecification userSpec = new com.hrms.superadmin.specs.UserSpecification();
        org.springframework.data.jpa.domain.Specification<User> spec =
            userSpec.filterUsers(companyId, null, isActiveFilter, designationFilter);

        org.springframework.data.domain.Page<User> userPage = userRepository.findAll(spec, pageable);

        List<EmployeeProfileResponse> responseDTOs = userPage.getContent().stream()
                .map(this::mapToEmployeeProfileResponse) // Assumes this mapper is in the class
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(responseDTOs, pageable, userPage.getTotalElements());
    }
}
