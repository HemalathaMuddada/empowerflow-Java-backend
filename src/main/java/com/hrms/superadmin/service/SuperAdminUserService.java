package com.hrms.superadmin.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.EmployeeProfileResponse; // Reusing
import com.hrms.superadmin.specs.UserSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrms.core.entity.Role; // Needed for Role entity
import com.hrms.core.repository.RoleRepository; // Needed for RoleRepository
import com.hrms.audit.service.AuditLogService; // Added
import com.hrms.hr.service.BadRequestException; // Assuming accessible from hr.service
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible from hr.service

import java.util.List;
import java.util.Set; // Added for Set
import java.util.stream.Collectors;


@Service
public class SuperAdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogService auditLogService;

    private static final Set<String> MANAGEABLE_ROLES = Set.of(
        "ROLE_EMPLOYEE", "ROLE_LEAD", "ROLE_HR", "ROLE_MANAGER"
    );

    @Transactional(readOnly = true)
    public Page<EmployeeProfileResponse> getAllUsersGlobal(
            Long filterByCompanyId, String filterByRoleName, Boolean filterByIsActive, Pageable pageable) {
        Specification<User> spec = UserSpecification.filterUsers(filterByCompanyId, filterByRoleName, filterByIsActive, null); // Assuming designationFilter is null for this specific existing method
        Page<User> userPage = userRepository.findAllWithCompanyAndRoles(spec, pageable); // Changed to use new method
        List<EmployeeProfileResponse> dtoList = userPage.getContent().stream()
                .map(this::mapToEmployeeProfileResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, userPage.getTotalElements());
    }

    // This mapper should be identical to the one in HREmployeeService etc.
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

    @Transactional
    public EmployeeProfileResponse grantSuperAdminRole(Long targetUserId, UserDetailsImpl currentSuperAdminUserDetails) {
        User userToGrant = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to grant Super Admin role not found with ID: " + targetUserId));

        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_SUPER_ADMIN not found in database. Critical setup issue."));

        if (userToGrant.getRoles().contains(superAdminRole)) {
            throw new BadRequestException("User with ID " + targetUserId + " already has ROLE_SUPER_ADMIN.");
        }

        userToGrant.getRoles().add(superAdminRole);
        User updatedUser = userRepository.save(userToGrant);

        String grantDetails = String.format("ROLE_SUPER_ADMIN granted to user '%s' (ID: %d).",
                                           updatedUser.getUsername(),
                                           updatedUser.getId());
        auditLogService.logEvent(
            currentSuperAdminUserDetails.getUsername(),
            currentSuperAdminUserDetails.getId(),
            "USER_ROLE_GRANT_SUPER_ADMIN",
            "User",
            String.valueOf(updatedUser.getId()),
            grantDetails,
            null,
            "SUCCESS"
        );
        return mapToEmployeeProfileResponse(updatedUser);
    }

    @Transactional
    public EmployeeProfileResponse revokeSuperAdminRole(Long targetUserId, UserDetailsImpl currentSuperAdminUserDetails) {
        User userToRevoke = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to revoke Super Admin role from not found with ID: " + targetUserId));

        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_SUPER_ADMIN not found in database. Critical setup issue."));

        if (!userToRevoke.getRoles().contains(superAdminRole)) {
            throw new BadRequestException("User with ID " + targetUserId + " does not have ROLE_SUPER_ADMIN.");
        }

        // Critical Safeguard: Prevent revoking self if this is the only Super Admin
        if (userToRevoke.getId().equals(currentSuperAdminUserDetails.getId())) {
            long superAdminCount = userRepository.countByRolesContains(superAdminRole);
            if (superAdminCount <= 1) {
                throw new BadRequestException("Cannot revoke your own Super Admin role as you are the only Super Admin remaining.");
            }
        }

        userToRevoke.getRoles().remove(superAdminRole);
        User updatedUser = userRepository.save(userToRevoke);

        String revokeDetails = String.format("ROLE_SUPER_ADMIN revoked from user '%s' (ID: %d).",
                                           updatedUser.getUsername(),
                                           updatedUser.getId());
        auditLogService.logEvent(
            currentSuperAdminUserDetails.getUsername(),
            currentSuperAdminUserDetails.getId(),
            "USER_ROLE_REVOKE_SUPER_ADMIN",
            "User",
            String.valueOf(updatedUser.getId()),
            revokeDetails,
            null,
            "SUCCESS"
        );
        return mapToEmployeeProfileResponse(updatedUser);
    }

    @Transactional
    public EmployeeProfileResponse assignRoleToUser(Long targetUserId, String roleName, UserDetailsImpl superAdminUserDetails) {
        if (!MANAGEABLE_ROLES.contains(roleName)) {
            throw new BadRequestException("Role '" + roleName + "' is not a standard role manageable by this function or is invalid.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + targetUserId));

        Role roleToAssign = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + roleName + "' not found in the system."));

        if (targetUser.getRoles().contains(roleToAssign)) {
            // Log or simply return current state as no change needed
            // logger.info("User {} already has role {}", targetUser.getUsername(), roleName);
            return mapToEmployeeProfileResponse(targetUser);
        }

        targetUser.getRoles().add(roleToAssign);
        User updatedUser = userRepository.save(targetUser);

        String details = String.format("Role '%s' assigned to user '%s' (ID: %d).",
                                       roleName, updatedUser.getUsername(), updatedUser.getId());
        auditLogService.logEvent(
            superAdminUserDetails.getUsername(),
            superAdminUserDetails.getId(),
            "USER_ROLE_ASSIGN",
            "User",
            String.valueOf(updatedUser.getId()),
            details,
            null,
            "SUCCESS"
        );
        return mapToEmployeeProfileResponse(updatedUser);
    }

    @Transactional
    public EmployeeProfileResponse removeRoleFromUser(Long targetUserId, String roleName, UserDetailsImpl superAdminUserDetails) {
        if (!MANAGEABLE_ROLES.contains(roleName)) {
            throw new BadRequestException("Role '" + roleName + "' is not a standard role manageable by this function or is invalid.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + targetUserId));

        Role roleToRemove = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + roleName + "' not found in the system."));

        if (!targetUser.getRoles().contains(roleToRemove)) {
            // Log or simply return current state
            // logger.info("User {} does not have role {}", targetUser.getUsername(), roleName);
            return mapToEmployeeProfileResponse(targetUser);
        }

        // Additional safeguard: Ensure user is not left with zero roles if roles are mandatory
        // For now, this is not implemented, assuming a user can exist without roles or this is handled elsewhere.
        // if (targetUser.getRoles().size() == 1 && targetUser.getRoles().contains(roleToRemove)) {
        //     throw new BadRequestException("Cannot remove the only role from a user. Assign another role first or deactivate the user.");
        // }


        targetUser.getRoles().remove(roleToRemove);
        User updatedUser = userRepository.save(targetUser);

        String details = String.format("Role '%s' revoked from user '%s' (ID: %d).",
                                       roleName, updatedUser.getUsername(), updatedUser.getId());
        auditLogService.logEvent(
            superAdminUserDetails.getUsername(),
            superAdminUserDetails.getId(),
            "USER_ROLE_REVOKE",
            "User",
            String.valueOf(updatedUser.getId()),
            details,
            null,
            "SUCCESS"
        );
        return mapToEmployeeProfileResponse(updatedUser);
    }
}
