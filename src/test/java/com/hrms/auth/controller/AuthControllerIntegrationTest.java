package com.hrms.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.core.entity.Company;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.CompanyRepository;
import com.hrms.core.repository.RoleRepository;
import com.hrms.core.repository.UserRepository;
import com.hrms.payload.request.LoginRequest;
import com.hrms.payload.response.JwtResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test") // Ensure application-test.properties is loaded
@Transactional // Rollback transactions after each test
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        // Clear specific repositories if needed, though @Transactional should handle it
        // userRepository.deleteAll();
        // roleRepository.deleteAll();
        // companyRepository.deleteAll();


        testCompany = companyRepository.save(new Company(null, "TestCo", "123 Street", null, null, true, null));

        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));
        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_EMPLOYEE")));


        testUser = new User();
        testUser.setUsername("testloginuser");
        testUser.setFirstName("Test");
        testUser.setLastName("UserLogin");
        testUser.setEmail("login@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setActive(true);
        testUser.setCompany(testCompany);
        testUser.setDateOfBirth(LocalDate.of(1990,1,1));
        testUser.setRoles(Set.of(userRole, employeeRole));
        userRepository.save(testUser);
    }

    @Test
    void login_success() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testloginuser", "password123");

        String responseString = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("testloginuser"))
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andReturn().getResponse().getContentAsString();

        JwtResponse jwtResponse = objectMapper.readValue(responseString, JwtResponse.class);
        assertThat(jwtResponse.getRoles()).contains("ROLE_USER", "ROLE_EMPLOYEE");
        assertThat(jwtResponse.getCompanyId()).isEqualTo(testCompany.getId());
    }

    @Test
    void login_invalidCredentials_usernameNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest("unknownuser", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // Or specific error code if customized
    }

    @Test
    void login_invalidCredentials_wrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testloginuser", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_userInactive() throws Exception {
        testUser.setActive(false);
        userRepository.save(testUser); // Update user to be inactive

        LoginRequest loginRequest = new LoginRequest("testloginuser", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // DisabledException maps to 401 by default
    }
}
