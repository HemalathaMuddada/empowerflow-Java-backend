package com.hrms.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.core.entity.Company;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.CompanyRepository;
import com.hrms.core.repository.RoleRepository;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.EmployeeProfileResponse;
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
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeePortalControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testEmployee;
    private String testEmployeeToken;
    private Company testCompany;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = companyRepository.save(new Company(null, "EmpPortalTestCo", "456 Test Ave", null, null, true, null));
        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_EMPLOYEE")));

        testEmployee = new User();
        testEmployee.setUsername("employee1");
        testEmployee.setFirstName("Emp");
        testEmployee.setLastName("One");
        testEmployee.setEmail("emp1@example.com");
        testEmployee.setPassword(passwordEncoder.encode("empPassword"));
        testEmployee.setActive(true);
        testEmployee.setCompany(testCompany);
        testEmployee.setDateOfBirth(LocalDate.of(1992, 2, 2));
        testEmployee.setDesignation("Engineer");
        testEmployee.setRoles(Set.of(employeeRole));
        userRepository.save(testEmployee);

        testEmployeeToken = getAuthToken("employee1", "empPassword");
    }

    private String getAuthToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String responseString = result.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseString, JwtResponse.class);
        return jwtResponse.getToken();
    }

    @Test
    void getEmployeeProfile_authenticated_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/employee/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testEmployeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("employee1"))
                .andExpect(jsonPath("$.firstName").value("Emp"))
                .andExpect(jsonPath("$.email").value("emp1@example.com"))
                .andExpect(jsonPath("$.companyName").value("EmpPortalTestCo"))
                .andExpect(jsonPath("$.designation").value("Engineer"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        EmployeeProfileResponse profileResponse = objectMapper.readValue(result.getResponse().getContentAsString(), EmployeeProfileResponse.class);
        assertThat(profileResponse.getRoles()).contains("ROLE_EMPLOYEE");
    }

    @Test
    void getEmployeeProfile_unauthenticated_fails() throws Exception {
        mockMvc.perform(get("/api/employee/profile"))
                .andExpect(status().isUnauthorized());
    }
}
