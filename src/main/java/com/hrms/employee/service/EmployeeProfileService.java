package com.hrms.employee.service;

import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.EmployeeProfileResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Corrected import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeProfileService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public EmployeeProfileResponse getCurrentEmployeeProfile(UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + currentUser.getId()));

        String companyName = (user.getCompany() != null) ? user.getCompany().getName() : null;

        List<String> roles = user.getRoles().stream()
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
                roles
        );
    }
}
