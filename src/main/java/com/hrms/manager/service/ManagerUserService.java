package com.hrms.manager.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import com.hrms.core.repository.CompanyRepository;
import com.hrms.core.repository.RoleRepository;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.EmployeeProfileResponse; // Reusing
import com.hrms.manager.payload.request.ManagerActionHRUserRequest;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Define custom exceptions or use a common exception package
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}

@Service
public class ManagerUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // CompanyRepository might not be needed if relying on managerUser.getCompany() directly
    // @Autowired
    // private CompanyRepository companyRepository;

    private static final String ROLE_HR_NAME = "ROLE_HR";

    @Transactional
    public EmployeeProfileResponse addHRUser(ManagerActionHRUserRequest request, UserDetailsImpl managerUserDetails) {
        User managerEntity = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Manager user not found: " + managerUserDetails.getUsername()));

        Company managerCompany = managerEntity.getCompany();
        if (managerCompany == null) {
            throw new IllegalStateException("Manager " + managerUserDetails.getUsername() + " is not associated with any company.");
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Password is required when adding a new HR user.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DataIntegrityViolationException("Username '" + request.getUsername() + "' already exists.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DataIntegrityViolationException("Email '" + request.getEmail() + "' is already in use.");
        }

        Role roleHr = roleRepository.findByName(ROLE_HR_NAME)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + ROLE_HR_NAME + "' not found. Cannot assign HR role."));

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setDateOfBirth(request.getDateOfBirth()); // Can be null if DTO allows
        newUser.setCompany(managerCompany); // HR user belongs to manager's company
        newUser.setRoles(Collections.singleton(roleHr));
        newUser.setActive(request.getIsActive() != null ? request.getIsActive() : true); // Default to true

        User savedUser = userRepository.save(newUser);
        return mapToEmployeeProfileResponse(savedUser);
    }

    @Transactional
    public EmployeeProfileResponse updateHRUser(Long hrUserId, ManagerActionHRUserRequest request, UserDetailsImpl managerUserDetails) {
        User managerEntity = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Manager user not found: " + managerUserDetails.getUsername()));
        Company managerCompany = managerEntity.getCompany();
        if (managerCompany == null) {
            throw new IllegalStateException("Manager " + managerUserDetails.getUsername() + " is not associated with any company.");
        }

        User hrUserToUpdate = userRepository.findById(hrUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HR User to update not found with ID: " + hrUserId));

        // Permission Check: Ensure HR user is in the same company as the manager
        if (hrUserToUpdate.getCompany() == null || !Objects.equals(hrUserToUpdate.getCompany().getId(), managerCompany.getId())) {
            throw new AccessDeniedException("Manager can only update HR users within their own company.");
        }
        // Ensure the user being updated actually has ROLE_HR
        if (hrUserToUpdate.getRoles().stream().noneMatch(role -> ROLE_HR_NAME.equals(role.getName()))) {
             throw new AccessDeniedException("User with ID " + hrUserId + " is not an HR user.");
        }

        // Update fields from request
        if (StringUtils.hasText(request.getFirstName())) {
            hrUserToUpdate.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            hrUserToUpdate.setLastName(request.getLastName());
        }
        // Prevent changing email if it's different and already exists for another user
        if (StringUtils.hasText(request.getEmail()) && !hrUserToUpdate.getEmail().equalsIgnoreCase(request.getEmail())) {
            if(userRepository.existsByEmail(request.getEmail())){
                 throw new DataIntegrityViolationException("Email '" + request.getEmail() + "' is already in use by another user.");
            }
            hrUserToUpdate.setEmail(request.getEmail());
        }
        if (request.getDateOfBirth() != null) {
            hrUserToUpdate.setDateOfBirth(request.getDateOfBirth());
        }
        if (StringUtils.hasText(request.getPassword())) {
            hrUserToUpdate.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getIsActive() != null) {
            hrUserToUpdate.setActive(request.getIsActive());
        }

        User updatedUser = userRepository.save(hrUserToUpdate);
        return mapToEmployeeProfileResponse(updatedUser);
    }

    @Transactional
    public EmployeeProfileResponse toggleHRUserActiveStatus(Long hrUserId, boolean isActive, UserDetailsImpl managerUserDetails) {
        User managerEntity = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Manager user not found: " + managerUserDetails.getUsername()));
        Company managerCompany = managerEntity.getCompany();
        if (managerCompany == null) {
            throw new IllegalStateException("Manager " + managerUserDetails.getUsername() + " is not associated with any company.");
        }

        User hrUserToUpdate = userRepository.findById(hrUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HR User to update status not found with ID: " + hrUserId));

        if (hrUserToUpdate.getCompany() == null || !Objects.equals(hrUserToUpdate.getCompany().getId(), managerCompany.getId())) {
            throw new AccessDeniedException("Manager can only update HR users within their own company.");
        }
        if (hrUserToUpdate.getRoles().stream().noneMatch(role -> ROLE_HR_NAME.equals(role.getName()))) {
             throw new AccessDeniedException("User with ID " + hrUserId + " is not an HR user.");
        }

        hrUserToUpdate.setActive(isActive);
        User updatedUser = userRepository.save(hrUserToUpdate);
        return mapToEmployeeProfileResponse(updatedUser);
    }


    private EmployeeProfileResponse mapToEmployeeProfileResponse(User user) {
        String companyName = (user.getCompany() != null) ? user.getCompany().getName() : null;
        List<String> roleNames = user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toList());
        // Assuming EmployeeProfileResponse DTO is updated to include designation
        return new EmployeeProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getDateOfBirth(),
                companyName,
                roleNames,
                user.getDesignation(), // This was added in a previous subtask to EmployeeProfileResponse
                user.isActive()        // New isActive field
        );
    }
}
