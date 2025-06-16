package com.hrms.hr.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.CompanyRepository;
import com.hrms.core.repository.RoleRepository;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.EmployeeProfileResponse;
import com.hrms.hr.payload.request.HRAddEmployeeRequest;
import com.hrms.hr.payload.request.HRUpdateEmployeeRequest;
import com.hrms.audit.service.AuditLogService;
import com.hrms.employee.core.enums.TaskStatus;
import com.hrms.employee.core.repository.TaskRepository;
import com.hrms.hr.payload.request.HRUpdateDesignationRequest;
import com.hrms.hr.payload.request.InitiateOffboardingRequest; // New DTO
import com.hrms.hr.payload.request.ReassignReporteesRequest;
import com.hrms.hr.payload.response.ReporteeReassignmentResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Define custom exceptions or use a common exception package
// class ResourceNotFoundException extends RuntimeException {
//    public ResourceNotFoundException(String message) { super(message); }
// }
//
// class BadRequestException extends RuntimeException {
//    public BadRequestException(String message) { super(message); }
// }

@Service
public class HREmployeeService {
    private static final Logger logger = LoggerFactory.getLogger(HREmployeeService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditLogService auditLogService;
    @Autowired private TaskRepository taskRepository;

    @Transactional
    public EmployeeProfileResponse addNewEmployee(HRAddEmployeeRequest request, UserDetailsImpl hrUserDetails) {
        User hrUserEntity = userRepository.findById(hrUserDetails.getId())
            .orElseThrow(() -> new ResourceNotFoundException("HR User performing action not found. ID: " + hrUserDetails.getId()));

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DataIntegrityViolationException("Username '" + request.getUsername() + "' already exists.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("Email '" + request.getEmail() + "' is already in use.");
        }

        Company targetCompany = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Target company not found with ID: " + request.getCompanyId()));

        if (hrUserEntity.getCompany() != null && !Objects.equals(hrUserEntity.getCompany().getId(), targetCompany.getId())) {
            throw new AccessDeniedException("HR users can only add employees to their own company.");
        }

        Set<Role> roles = new HashSet<>(roleRepository.findByNameIn(request.getRoleNames()));
        if (roles.size() != request.getRoleNames().size() || roles.isEmpty()) {
            throw new BadRequestException("Invalid or empty role names provided. Found " + roles.size() + " roles for names: " + request.getRoleNames());
        }

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + request.getManagerId()));
            if (manager.getCompany() == null || !Objects.equals(manager.getCompany().getId(), targetCompany.getId())) {
                throw new BadRequestException("Assigned manager (ID: " + request.getManagerId() + ") does not belong to the target company (ID: " + targetCompany.getId() + ").");
            }
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setDateOfBirth(request.getDateOfBirth());
        newUser.setCompany(targetCompany);
        newUser.setRoles(roles);
        newUser.setManager(manager);
        newUser.setActive(true);

        User savedUser = userRepository.save(newUser);
        return mapToEmployeeProfileResponse(savedUser, null, null); // No offboarding specific data here
    }

    @Transactional
    public EmployeeProfileResponse updateEmployeeDetails(Long employeeId, HRUpdateEmployeeRequest request, UserDetailsImpl hrUserDetails) {
        User hrUserEntity = userRepository.findById(hrUserDetails.getId())
            .orElseThrow(() -> new ResourceNotFoundException("HR User performing action not found. ID: " + hrUserDetails.getId()));

        User userToUpdate = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        if (hrUserEntity.getCompany() != null) {
            if (userToUpdate.getCompany() == null || !Objects.equals(userToUpdate.getCompany().getId(), hrUserEntity.getCompany().getId())) {
                throw new AccessDeniedException("HR users can only update employees within their own company.");
            }
        }

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            userToUpdate.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            userToUpdate.setLastName(request.getLastName());
        }
        if (request.getDateOfBirth() != null) {
            userToUpdate.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getIsActive() != null) {
            userToUpdate.setActive(request.getIsActive());
        }

        if (request.getManagerId() != null) {
            User newManager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("New manager not found with ID: " + request.getManagerId()));
            Company employeeCompany = userToUpdate.getCompany();
            if (employeeCompany != null) {
                 if (newManager.getCompany() == null || !Objects.equals(newManager.getCompany().getId(), employeeCompany.getId())) {
                    throw new BadRequestException("New manager (ID: " + request.getManagerId() + ") must belong to the same company as the employee (Company ID: " + employeeCompany.getId() + ").");
                }
            } else {
                if (newManager.getCompany() != null) {
                    throw new BadRequestException("Cannot assign a company-specific manager (ID: " + request.getManagerId() + ") to an employee not assigned to any company.");
                }
            }
            userToUpdate.setManager(newManager);
        }

        if (request.getCompanyId() != null) {
            if (hrUserEntity.getCompany() != null && !Objects.equals(request.getCompanyId(), hrUserEntity.getCompany().getId())) {
                 throw new AccessDeniedException("HR users cannot assign employees to a different company than their own.");
            }
            Company newCompany = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("New company not found with ID: " + request.getCompanyId()));
            userToUpdate.setCompany(newCompany);
        }

        User updatedUser = userRepository.save(userToUpdate);
        return mapToEmployeeProfileResponse(updatedUser, null, null);
    }

    @Transactional
    public EmployeeProfileResponse changeEmployeeDesignation(Long employeeId, HRUpdateDesignationRequest request, UserDetailsImpl hrUserDetails) {
        User hrUserEntity = userRepository.findById(hrUserDetails.getId())
            .orElseThrow(() -> new ResourceNotFoundException("HR User performing action not found. ID: " + hrUserDetails.getId()));

        User userToUpdate = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        if (hrUserEntity.getCompany() != null) {
            if (userToUpdate.getCompany() == null || !Objects.equals(userToUpdate.getCompany().getId(), hrUserEntity.getCompany().getId())) {
                throw new AccessDeniedException("HR users can only change designation for employees within their own company.");
            }
        }

        userToUpdate.setDesignation(request.getNewDesignation());
        User updatedUser = userRepository.save(userToUpdate);
        return mapToEmployeeProfileResponse(updatedUser, null, null);
    }

    @Transactional
    public EmployeeProfileResponse initiateOffboarding(Long employeeId, InitiateOffboardingRequest offboardingRequest, UserDetailsImpl hrUserDetails) {
        User hrUserEntity = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("HR User performing action not found with ID: " + hrUserDetails.getId()));

        User userToOffboard = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee to offboard not found with ID: " + employeeId));

        if (Objects.equals(userToOffboard.getId(), hrUserDetails.getId())) {
            throw new BadRequestException("Cannot initiate offboarding for your own account via this operation.");
        }

        if (hrUserEntity.getCompany() != null) {
            if (userToOffboard.getCompany() == null ||
                !Objects.equals(userToOffboard.getCompany().getId(), hrUserEntity.getCompany().getId())) {
                throw new AccessDeniedException("HR users can only initiate offboarding for employees within their own company.");
            }
        }

        boolean targetIsSuperAdmin = userToOffboard.getRoles().stream()
                                     .anyMatch(role -> "ROLE_SUPER_ADMIN".equals(role.getName()));
        if (targetIsSuperAdmin) {
            boolean actorIsSuperAdmin = hrUserEntity.getRoles().stream()
                                        .anyMatch(role -> "ROLE_SUPER_ADMIN".equals(role.getName()));
            if (!actorIsSuperAdmin) {
                throw new AccessDeniedException("Only a Super Admin can initiate offboarding for another Super Admin account.");
            }
        }

        userToOffboard.setActive(false); // Core action of deactivation
        userToOffboard.setOffboardingDate(offboardingRequest.getOffboardingDate());
        userToOffboard.setReasonForLeaving(offboardingRequest.getReasonForLeaving());
        userToOffboard.setOffboardingCommentsByHR(offboardingRequest.getHrComments());

        User savedUser = userRepository.save(userToOffboard);

        List<TaskStatus> openTaskStatuses = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);
        long openTasksCount = taskRepository.countByAssignedToAndStatusInAndAutoClosedAtIsNull(savedUser, openTaskStatuses);
        logger.info("Employee {} (ID: {}) being offboarded has {} open (non-auto-closed) tasks.",
                    savedUser.getUsername(), savedUser.getId(), openTasksCount);

        boolean isManagerOfActive = userRepository.existsByManagerIdAndIsActiveTrue(savedUser.getId());
        logger.info("Employee {} (ID: {}) being offboarded is manager of active reportees: {}.",
                    savedUser.getUsername(), savedUser.getId(), isManagerOfActive);


        String auditDetails = String.format("Offboarding initiated for user '%s' (ID: %d). Offboarding Date: %s, Reason: %s. Open tasks: %d. Is Manager of Active Reportees: %s.",
                                       savedUser.getUsername(),
                                       savedUser.getId(),
                                       Objects.toString(savedUser.getOffboardingDate(), "N/A"),
                                       Objects.toString(savedUser.getReasonForLeaving(), "N/A"),
                                       openTasksCount,
                                       isManagerOfActive ? "Yes" : "No");
        auditLogService.logEvent(
            hrUserDetails.getUsername(),
            hrUserDetails.getId(),
            "USER_OFFBOARDING_INITIATED",
            "User",
            String.valueOf(savedUser.getId()),
            auditDetails,
            null,
            "SUCCESS"
        );

        return mapToEmployeeProfileResponse(savedUser, openTasksCount, isManagerOfActive);
    }

    @Transactional(readOnly = true)
    public List<EmployeeProfileResponse> getEmployeesByCompanyAndStatus(Long companyId, Boolean isActiveFilter, UserDetailsImpl hrUserDetails) {
        User hrUserEntity = userRepository.findById(hrUserDetails.getId())
            .orElseThrow(() -> new ResourceNotFoundException("HR User not found: " + hrUserDetails.getId()));

        if (hrUserEntity.getCompany() != null && !Objects.equals(hrUserEntity.getCompany().getId(), companyId)) {
             if (companyId != null) {
                 throw new AccessDeniedException("You are not authorized to view employees for this company.");
             }
        }

        if (companyId == null && hrUserEntity.getCompany() != null) {
            throw new BadRequestException("Company ID must be provided for company-specific HR.");
        }

        if (companyId != null) {
             companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));
        }

        List<User> users;
        if (isActiveFilter != null) {
            if (companyId != null) {
                users = userRepository.findByCompanyIdAndIsActive(companyId, isActiveFilter);
            } else {
                 if (hrUserEntity.getCompany() != null) {
                     throw new AccessDeniedException("You must specify a company or be a global HR to list unassigned users.");
                 }
                 users = userRepository.findByCompanyIsNullAndIsActive(isActiveFilter);
            }
        } else {
            if (companyId != null) {
                users = userRepository.findByCompanyId(companyId);
            } else {
                if (hrUserEntity.getCompany() != null) {
                     throw new AccessDeniedException("You must specify a company or be a global HR to list unassigned users.");
                }
                users = userRepository.findByCompanyIsNull();
            }
        }

        return users.stream()
                .map(user -> mapToEmployeeProfileResponse(user, null, null)) // Offboarding specific fields not relevant here
                .collect(Collectors.toList());
    }

    @Transactional
    public ReporteeReassignmentResponse reassignDirectReportees(ReassignReporteesRequest request, UserDetailsImpl hrUserDetails) {
        User hrUserEntity = userRepository.findById(hrUserDetails.getId())
            .orElseThrow(() -> new ResourceNotFoundException("HR User performing action not found. ID: " + hrUserDetails.getId()));

        User offboardedManager = userRepository.findById(request.getOffboardedManagerId())
            .orElseThrow(() -> new ResourceNotFoundException("Offboarding Manager not found with ID: " + request.getOffboardedManagerId()));

        User newManager = userRepository.findById(request.getNewManagerId())
            .orElseThrow(() -> new ResourceNotFoundException("New Manager not found with ID: " + request.getNewManagerId()));

        if (Objects.equals(offboardedManager.getId(), newManager.getId())) {
            throw new BadRequestException("Offboarded manager and new manager cannot be the same person.");
        }
        if (!newManager.isActive()) {
            throw new BadRequestException("New manager (ID: " + newManager.getId() + ") is not an active user.");
        }

        Company hrCompany = hrUserEntity.getCompany();
        if (hrCompany != null) {
            if (offboardedManager.getCompany() == null || !Objects.equals(offboardedManager.getCompany().getId(), hrCompany.getId())) {
                throw new AccessDeniedException("HR cannot reassign reportees for a manager outside their own company.");
            }
            if (newManager.getCompany() == null || !Objects.equals(newManager.getCompany().getId(), hrCompany.getId())) {
                throw new AccessDeniedException("HR cannot assign reportees to a new manager outside their own company.");
            }
        }

        if (!Objects.equals(offboardedManager.getCompany(), newManager.getCompany())) {
            logger.warn("Reassigning reportees from manager {} (Company: {}) to manager {} (Company: {}). Reportees company will NOT be changed by this operation.",
                offboardedManager.getUsername(), offboardedManager.getCompany() !=null ? offboardedManager.getCompany().getName() : "N/A",
                newManager.getUsername(), newManager.getCompany() !=null ? newManager.getCompany().getName() : "N/A");
        }

        List<User> directReportees = userRepository.findByManagerIdAndIsActiveTrue(request.getOffboardedManagerId());

        if (directReportees.isEmpty()) {
            String message = "No active direct reportees to reassign for manager " + offboardedManager.getUsername() + " (ID: " + request.getOffboardedManagerId() + ").";
            logger.info(message);
            return new ReporteeReassignmentResponse(0, message);
        }

        for (User reportee : directReportees) {
            reportee.setManager(newManager);
            userRepository.save(reportee);
        }

        String auditDetails = String.format("Reportees of manager %s (ID: %d) reassigned to new manager %s (ID: %d). %d active reportees affected.",
                                           offboardedManager.getUsername(), offboardedManager.getId(),
                                           newManager.getUsername(), newManager.getId(), directReportees.size());
        auditLogService.logEvent(
            hrUserDetails.getUsername(),
            hrUserDetails.getId(),
            "DIRECT_REPORTEES_REASSIGNED",
            "User",
            String.valueOf(offboardedManager.getId()),
            auditDetails,
            null,
            "SUCCESS"
        );

        String successMessage = String.format("Successfully reassigned %d active direct reportees from %s to %s.",
                                            directReportees.size(), offboardedManager.getUsername(), newManager.getUsername());
        logger.info(successMessage);
        return new ReporteeReassignmentResponse(directReportees.size(), successMessage);
    }

    private EmployeeProfileResponse mapToEmployeeProfileResponse(User user, Long openTasksCount, Boolean isManagerWithActiveReportees) {
        String companyName = (user.getCompany() != null) ? user.getCompany().getName() : null;
        List<String> roleNames = user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toList());
        EmployeeProfileResponse dto = new EmployeeProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getDateOfBirth(),
                companyName,
                roleNames,
                user.getDesignation(),
                user.isActive(),
                openTasksCount,
                isManagerWithActiveReportees,
                user.getOffboardingDate(),      // New field
                user.getReasonForLeaving(),     // New field
                user.getOffboardingCommentsByHR() // New field
        );
        return dto;
    }
}
