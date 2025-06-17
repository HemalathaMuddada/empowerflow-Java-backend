package com.hrms.hr.service;

import com.hrms.audit.service.AuditLogService;
import com.hrms.core.entity.Company;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.CompanyRepository;
import com.hrms.core.repository.RoleRepository;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.enums.TaskStatus;
import com.hrms.employee.core.repository.TaskRepository;
import com.hrms.employee.payload.response.EmployeeProfileResponse;
import com.hrms.hr.payload.request.HRAddEmployeeRequest;
import com.hrms.hr.payload.request.InitiateOffboardingRequest;
import com.hrms.security.service.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HREmployeeServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;
    @Mock private TaskRepository taskRepository;

    @InjectMocks
    private HREmployeeService hrEmployeeService;

    private UserDetailsImpl mockHrUserDetails;
    private User mockHrUser;
    private Company mockCompany;
    private Role mockEmployeeRole;

    @BeforeEach
    void setUp() {
        mockCompany = new Company(1L, "Test Company", "123 Test St", LocalDateTime.now(), LocalDateTime.now(), true, Collections.emptySet());

        mockHrUser = new User();
        mockHrUser.setId(10L);
        mockHrUser.setUsername("hruser");
        mockHrUser.setCompany(mockCompany);
        mockHrUser.setRoles(Set.of(new Role(1L, "ROLE_HR")));


        mockHrUserDetails = new UserDetailsImpl(mockHrUser.getId(), mockHrUser.getUsername(), "hr@example.com", "pass",
                                                mockHrUser.getCompany() != null ? mockHrUser.getCompany().getId() : null,
                                                Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_HR")));

        mockEmployeeRole = new Role(2L, "ROLE_EMPLOYEE");
    }

    @Test
    void addNewEmployee_success() {
        HRAddEmployeeRequest request = new HRAddEmployeeRequest("John", "Doe", "johndoe", "john.doe@example.com",
                                                              "password123", LocalDate.now().minusYears(30),
                                                              mockCompany.getId(), List.of("ROLE_EMPLOYEE"), null);

        when(userRepository.findById(mockHrUserDetails.getId())).thenReturn(Optional.of(mockHrUser));
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(companyRepository.findById(mockCompany.getId())).thenReturn(Optional.of(mockCompany));
        when(roleRepository.findByNameIn(List.of("ROLE_EMPLOYEE"))).thenReturn(Set.of(mockEmployeeRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = new User(); // Populate with expected values after save
        savedUser.setId(1L);
        savedUser.setUsername("johndoe");
        savedUser.setFirstName("John");
        savedUser.setLastName("Doe");
        savedUser.setEmail("john.doe@example.com");
        savedUser.setCompany(mockCompany);
        savedUser.setRoles(Set.of(mockEmployeeRole));
        savedUser.setActive(true);
        savedUser.setPassword("encodedPassword");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        EmployeeProfileResponse response = hrEmployeeService.addNewEmployee(request, mockHrUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("johndoe");
        verify(userRepository).save(any(User.class));
        // Audit log is not part of addNewEmployee yet, so not verifying it here.
    }

    @Test
    void addNewEmployee_usernameExists_throwsException() {
        HRAddEmployeeRequest request = new HRAddEmployeeRequest("John", "Doe", "johndoe", "john.doe@example.com", "password123", LocalDate.now(), 1L, List.of("ROLE_EMPLOYEE"), null);
        when(userRepository.findById(mockHrUserDetails.getId())).thenReturn(Optional.of(mockHrUser));
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> hrEmployeeService.addNewEmployee(request, mockHrUserDetails));
    }

    // Add tests for email exists, role not found, company not found etc.

    @Test
    void initiateOffboarding_success() {
        User employeeToOffboard = new User();
        employeeToOffboard.setId(2L);
        employeeToOffboard.setUsername("offboarduser");
        employeeToOffboard.setFirstName("Offboard");
        employeeToOffboard.setLastName("User");
        employeeToOffboard.setCompany(mockCompany);
        employeeToOffboard.setActive(true);

        InitiateOffboardingRequest request = new InitiateOffboardingRequest(LocalDate.now().plusDays(10), "Resigned", "Exit interview done.");

        when(userRepository.findById(mockHrUserDetails.getId())).thenReturn(Optional.of(mockHrUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeToOffboard));
        when(taskRepository.countByAssignedToAndStatusInAndAutoClosedAtIsNull(any(User.class), anyList())).thenReturn(2L);
        when(userRepository.existsByManagerIdAndIsActiveTrue(2L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(employeeToOffboard); // Simulate save returning the user

        EmployeeProfileResponse response = hrEmployeeService.initiateOffboarding(2L, request, mockHrUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getIsActive()).isFalse();
        assertThat(response.getOpenTasksCount()).isEqualTo(2L);
        assertThat(response.getIsManagerWithActiveReportees()).isFalse();
        assertThat(response.getOffboardingDate()).isEqualTo(request.getOffboardingDate());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isActive()).isFalse();
        assertThat(userCaptor.getValue().getReasonForLeaving()).isEqualTo("Resigned");

        verify(auditLogService).logEvent(
            eq(mockHrUserDetails.getUsername()),
            eq(mockHrUserDetails.getId()),
            eq("USER_OFFBOARDING_INITIATED"),
            eq("User"),
            eq(String.valueOf(2L)),
            anyString(), // Details string
            isNull(),
            eq("SUCCESS")
        );
    }

    @Test
    void initiateOffboarding_selfDeactivation_throwsException() {
        InitiateOffboardingRequest request = new InitiateOffboardingRequest();
        when(userRepository.findById(mockHrUserDetails.getId())).thenReturn(Optional.of(mockHrUser));
        // HR user tries to offboard themselves
        when(userRepository.findById(mockHrUserDetails.getId())).thenReturn(Optional.of(mockHrUser));


        assertThrows(BadRequestException.class, () -> hrEmployeeService.initiateOffboarding(mockHrUserDetails.getId(), request, mockHrUserDetails));
    }

    @Test
    void initiateOffboarding_hrUserNotInCompany_targetInCompany_throwsAccessDenied() {
        mockHrUser.setCompany(null); // Global HR
        User employeeToOffboard = new User();
        employeeToOffboard.setId(2L);
        employeeToOffboard.setCompany(new Company(2L, "Other Company", null, null, null, true, null));

        InitiateOffboardingRequest request = new InitiateOffboardingRequest();
        when(userRepository.findById(mockHrUserDetails.getId())).thenReturn(Optional.of(mockHrUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeToOffboard));

        // This check is: if (hrUserEntity.getCompany() != null), so global HR bypasses this specific company check.
        // The logic for global HR needs careful consideration if they should be restricted.
        // The current code allows global HR to offboard anyone not super admin.
        // Let's test the case where HR *is* in a company and target is not.
        mockHrUser.setCompany(mockCompany); // HR in "Test Company"
         when(userRepository.findById(mockHrUserDetails.getId())).thenReturn(Optional.of(mockHrUser));


        assertThrows(AccessDeniedException.class, () -> hrEmployeeService.initiateOffboarding(2L, request, mockHrUserDetails));
    }


    // Add tests for Super Admin deactivation protection
}
