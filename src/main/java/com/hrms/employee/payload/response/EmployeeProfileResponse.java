package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private LocalDate dateOfBirth;
    private String companyName;
    private List<String> roles;
}
