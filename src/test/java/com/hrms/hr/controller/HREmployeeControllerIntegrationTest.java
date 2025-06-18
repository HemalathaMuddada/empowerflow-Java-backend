package com.hrms.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.core.entity.Company;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.CompanyRepository;
import com.hrms.core.repository.RoleRepository;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.EmployeeProfileResponse;
import com.hrms.hr.payload.request.HRAddEmployeeRequest;
import com.hrms.payload.request.LoginRequest;
import com.hrms.payload.response.JwtResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HREmployeeControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String hrUserToken;
    private String employeeUserToken;
    private Company testCompany;
    private Role hrRole;
    private Role employeeRole;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = companyRepository.save(new Company(null, "HRTestCo", "789 HR St", null, null, true, null));
        hrRole = roleRepository.findByName("ROLE_HR").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_HR")));
        employeeRole = roleRepository.findByName("ROLE_EMPLOYEE").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_EMPLOYEE")));

        User hrUser = new User(null, "hra", "HR", "Admin", "hra@example.com", passwordEncoder.encode("hrPassword"),
                               LocalDate.of(1980, 5, 5), true, testCompany, Set.of(hrRole, employeeRole), null,
                               "HR Manager", null, null, null, null, null, null, null);
        userRepository.save(hrUser);
        hrUserToken = getAuthToken("hra", "hrPassword");

        User empUser = new User(null, "empbasic", "Basic", "User", "basic@example.com", passwordEncoder.encode("empPassword"),
                                LocalDate.of(1995, 6, 6), true, testCompany, Set.of(employeeRole), null,
                                "Worker", null, null, null, null, null, null, null);
        userRepository.save(empUser);
        employeeUserToken = getAuthToken("empbasic", "empPassword");
    }

    private String getAuthToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), JwtResponse.class).getToken();
    }

    @Test
    void addNewEmployee_asHR_success() throws Exception {
        HRAddEmployeeRequest addRequest = new HRAddEmployeeRequest(
                "New", "Hire", "newhire01", "new.hire@example.com", "newHirePass",
                LocalDate.of(1998, 3, 3), testCompany.getId(), List.of("ROLE_EMPLOYEE"), null
        );

        mockMvc.perform(post("/api/hr/employees/add")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newhire01"))
                .andExpect(jsonPath("$.email").value("new.hire@example.com"))
                .andExpect(jsonPath("$.companyName").value(testCompany.getName()));

        User createdUser = userRepository.findByUsername("newhire01").orElseThrow();
        assertThat(createdUser).isNotNull();
        assertThat(passwordEncoder.matches("newHirePass", createdUser.getPassword())).isTrue();
    }

    @Test
    void addNewEmployee_asNonHR_forbidden() throws Exception {
        HRAddEmployeeRequest addRequest = new HRAddEmployeeRequest(
                "Attempt", "User", "attempt01", "attempt@example.com", "attemptPass",
                LocalDate.of(2000, 1, 1), testCompany.getId(), List.of("ROLE_EMPLOYEE"), null
        );

        mockMvc.perform(post("/api/hr/employees/add")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeUserToken) // Using employee token
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void addNewEmployee_duplicateUsername_conflict() throws Exception {
        // First, add an employee successfully
        HRAddEmployeeRequest firstRequest = new HRAddEmployeeRequest(
            "First", "Emp", "duplicateUser", "first.emp@example.com", "password",
            LocalDate.of(1990, 1, 1), testCompany.getId(), List.of("ROLE_EMPLOYEE"), null
        );
        mockMvc.perform(post("/api/hr/employees/add")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Then, attempt to add another employee with the same username
        HRAddEmployeeRequest secondRequest = new HRAddEmployeeRequest(
            "Second", "Emp", "duplicateUser", "second.emp@example.com", "password",
            LocalDate.of(1991, 1, 1), testCompany.getId(), List.of("ROLE_EMPLOYEE"), null
        );
        mockMvc.perform(post("/api/hr/employees/add")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict()); // Expect 409 Conflict
    }
}
