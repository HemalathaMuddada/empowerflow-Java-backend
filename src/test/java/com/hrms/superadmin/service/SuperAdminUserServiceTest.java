package com.hrms.superadmin.service;

import com.hrms.audit.service.AuditLogService;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.RoleRepository;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.EmployeeProfileResponse;
import com.hrms.hr.service.BadRequestException; // Assuming this is the one used
import com.hrms.hr.service.ResourceNotFoundException; // Assuming this is the one used
import com.hrms.security.service.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private SuperAdminUserService superAdminUserService;

    private UserDetailsImpl mockSuperAdminUserDetails;
    private User mockTargetUser;
    private Role mockSuperAdminRole;
    private Role mockEmployeeRole;

    @BeforeEach
    void setUp() {
        mockSuperAdminUserDetails = new UserDetailsImpl(1L, "superadmin", "super@admin.com", "pass", null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        mockSuperAdminRole = new Role(100L, "ROLE_SUPER_ADMIN");
        mockEmployeeRole = new Role(1L, "ROLE_EMPLOYEE");

        mockTargetUser = new User();
        mockTargetUser.setId(2L);
        mockTargetUser.setUsername("targetuser");
        mockTargetUser.setRoles(new HashSet<>(Set.of(mockEmployeeRole))); // Initially an employee
    }

    @Test
    void grantSuperAdminRole_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockTargetUser));
        when(roleRepository.findByName("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(mockSuperAdminRole));
        when(userRepository.save(any(User.class))).thenReturn(mockTargetUser);

        EmployeeProfileResponse response = superAdminUserService.grantSuperAdminRole(2L, mockSuperAdminUserDetails);

        assertThat(response).isNotNull();
        assertThat(mockTargetUser.getRoles()).contains(mockSuperAdminRole);
        verify(userRepository).save(mockTargetUser);
        verify(auditLogService).logEvent(eq("superadmin"), eq(1L), eq("USER_ROLE_GRANT_SUPER_ADMIN"), eq("User"), eq("2"), anyString(), isNull(), eq("SUCCESS"));
    }

    @Test
    void grantSuperAdminRole_userAlreadyHasRole_throwsBadRequestException() {
        mockTargetUser.getRoles().add(mockSuperAdminRole); // User already has the role
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockTargetUser));
        when(roleRepository.findByName("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(mockSuperAdminRole));

        assertThrows(BadRequestException.class, () -> superAdminUserService.grantSuperAdminRole(2L, mockSuperAdminUserDetails));
    }

    @Test
    void grantSuperAdminRole_targetUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> superAdminUserService.grantSuperAdminRole(2L, mockSuperAdminUserDetails));
    }


    @Test
    void revokeSuperAdminRole_success() {
        mockTargetUser.getRoles().add(mockSuperAdminRole); // User has the role to revoke
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockTargetUser));
        when(roleRepository.findByName("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(mockSuperAdminRole));
        when(userRepository.save(any(User.class))).thenReturn(mockTargetUser);

        EmployeeProfileResponse response = superAdminUserService.revokeSuperAdminRole(2L, mockSuperAdminUserDetails);

        assertThat(response).isNotNull();
        assertThat(mockTargetUser.getRoles()).doesNotContain(mockSuperAdminRole);
        verify(userRepository).save(mockTargetUser);
        verify(auditLogService).logEvent(eq("superadmin"), eq(1L), eq("USER_ROLE_REVOKE_SUPER_ADMIN"), eq("User"), eq("2"), anyString(), isNull(), eq("SUCCESS"));
    }

    @Test
    void revokeSuperAdminRole_userDoesNotHaveRole_throwsBadRequestException() {
        // mockTargetUser does not have ROLE_SUPER_ADMIN by default setup
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockTargetUser));
        when(roleRepository.findByName("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(mockSuperAdminRole));

        assertThrows(BadRequestException.class, () -> superAdminUserService.revokeSuperAdminRole(2L, mockSuperAdminUserDetails));
    }

    @Test
    void revokeSuperAdminRole_selfRevokeLastSuperAdmin_throwsBadRequestException() {
        // Current superAdminUserDetails has ID 1L. Let's make targetUser the same.
        mockTargetUser.setId(1L);
        mockTargetUser.getRoles().add(mockSuperAdminRole);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockTargetUser));
        when(roleRepository.findByName("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(mockSuperAdminRole));
        when(userRepository.countByRolesContains(mockSuperAdminRole)).thenReturn(1L); // Only one super admin

        assertThrows(BadRequestException.class, () -> superAdminUserService.revokeSuperAdminRole(1L, mockSuperAdminUserDetails));
    }

    @Test
    void assignRoleToUser_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockTargetUser));
        Role roleToAssign = new Role(3L, "ROLE_MANAGER");
        when(roleRepository.findByName("ROLE_MANAGER")).thenReturn(Optional.of(roleToAssign));
        when(userRepository.save(any(User.class))).thenReturn(mockTargetUser);

        EmployeeProfileResponse response = superAdminUserService.assignRoleToUser(2L, "ROLE_MANAGER", mockSuperAdminUserDetails);
        assertThat(mockTargetUser.getRoles()).contains(roleToAssign);
        verify(auditLogService).logEvent(anyString(),anyLong(),eq("USER_ROLE_ASSIGN"),anyString(),anyString(),anyString(),isNull(),eq("SUCCESS"));
    }

    @Test
    void assignRoleToUser_invalidRoleName_throwsBadRequest() {
         assertThrows(BadRequestException.class, () -> superAdminUserService.assignRoleToUser(2L, "ROLE_INVALID", mockSuperAdminUserDetails));
    }


    @Test
    void removeRoleFromUser_success() {
        Role roleToRevoke = mockEmployeeRole; // targetUser has ROLE_EMPLOYEE by default
        assertThat(mockTargetUser.getRoles()).contains(roleToRevoke);

        when(userRepository.findById(2L)).thenReturn(Optional.of(mockTargetUser));
        when(roleRepository.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(roleToRevoke));
        when(userRepository.save(any(User.class))).thenReturn(mockTargetUser);

        EmployeeProfileResponse response = superAdminUserService.removeRoleFromUser(2L, "ROLE_EMPLOYEE", mockSuperAdminUserDetails);
        assertThat(mockTargetUser.getRoles()).doesNotContain(roleToRevoke);
        verify(auditLogService).logEvent(anyString(),anyLong(),eq("USER_ROLE_REVOKE"),anyString(),anyString(),anyString(),isNull(),eq("SUCCESS"));
    }
}
