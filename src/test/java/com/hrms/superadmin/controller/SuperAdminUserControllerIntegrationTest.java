package com.hrms.superadmin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.data.domain.Page; // For Page type reference
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SuperAdminUserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String superAdminToken;
    private String hrUserToken;

    @BeforeEach
    void setUp() throws Exception {
        Company compA = companyRepository.save(new Company(null, "CompA", "AddrA", null, null, true, null));
        Company compB = companyRepository.save(new Company(null, "CompB", "AddrB", null, null, true, null));

        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_SUPER_ADMIN")));
        Role hrRole = roleRepository.findByName("ROLE_HR").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_HR")));
        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE").orElseGet(() -> roleRepository.save(new Role(null, "ROLE_EMPLOYEE")));

        User saUser = new User(null, "super", "Super", "Admin", "super@example.com", passwordEncoder.encode("superPass"),
                               LocalDate.of(1970,1,1), true, null, Set.of(superAdminRole, employeeRole), null,
                               "System Lord", null, null, null, null, null, null, null);
        userRepository.save(saUser);
        superAdminToken = getAuthToken("super", "superPass");

        User hrUser = new User(null, "hr.comp.a", "HR", "PersonA", "hr.a@example.com", passwordEncoder.encode("hrPassA"),
                               LocalDate.of(1980,1,1), true, compA, Set.of(hrRole, employeeRole), null,
                               "HR Manager", null, null, null, null, null, null, null);
        userRepository.save(hrUser);
        hrUserToken = getAuthToken("hr.comp.a", "hrPassA");

        userRepository.save(new User(null, "emp.a1", "EmpA", "One", "empa1@example.com", passwordEncoder.encode("pass"),
                               LocalDate.of(1990,1,1), true, compA, Set.of(employeeRole), hrUser,
                               "Engineer", null, null, null, null, null, null, null));
        userRepository.save(new User(null, "emp.b1", "EmpB", "One", "empb1@example.com", passwordEncoder.encode("pass"),
                               LocalDate.of(1991,1,1), true, compB, Set.of(employeeRole), null,
                               "Analyst", null, null, null, null, null, null, null));
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
    void getAllUsersGlobally_asSuperAdmin_successWithPagination() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/super-admin/users/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                .param("page", "0")
                .param("size", "2") // Request 2 users
                .param("sort", "username,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(userRepository.count())) // Total users created in setup
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.numberOfElements").value(2)) // Should get 2 users on page 0
                .andReturn();

        // For more specific content assertion, deserialize the Page<EmployeeProfileResponse>
        // This requires a custom deserializer for Spring's PageImpl or a wrapper type.
        // For now, checking array presence and element count is a good start.
        // String responseContent = result.getResponse().getContentAsString();
        // TypeReference<RestPageImpl<EmployeeProfileResponse>> typeRef = new TypeReference<>() {};
        // Page<EmployeeProfileResponse> pageResponse = objectMapper.readValue(responseContent, typeRef);
        // assertThat(pageResponse.getContent()).hasSize(2);
        // assertThat(pageResponse.getContent().get(0).getUsername()).isEqualTo("emp.a1"); // Assuming this is first by username asc
    }

    @Test
    void getAllUsersGlobally_asNonSuperAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/super-admin/users/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrUserToken) // Using HR user token
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isForbidden());
    }
}

// Helper class for deserializing Page<T> if needed for deeper assertions
// import com.fasterxml.jackson.annotation.JsonCreator;
// import com.fasterxml.jackson.annotation.JsonProperty;
// import com.fasterxml.jackson.databind.JsonNode;
// import org.springframework.data.domain.PageImpl;
// import org.springframework.data.domain.PageRequest;
// import java.util.List;
// class RestPageImpl<T> extends PageImpl<T> {
//    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
//    public RestPageImpl(@JsonProperty("content") List<T> content,
//                        @JsonProperty("number") int number,
//                        @JsonProperty("size") int size,
//                        @JsonProperty("totalElements") Long totalElements,
//                        @JsonProperty("pageable") JsonNode pageable,
//                        @JsonProperty("last") boolean last,
//                        @JsonProperty("totalPages") int totalPages,
//                        @JsonProperty("sort") JsonNode sort,
//                        @JsonProperty("first") boolean first,
//                        @JsonProperty("numberOfElements") int numberOfElements) {
//        super(content, PageRequest.of(number, size), totalElements);
//    }
// }
