package com.hrms.hr.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.core.repository.CompanyRepository; // Needed if associating with a specific company
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Holiday;
import com.hrms.employee.core.repository.HolidayRepository;
import com.hrms.employee.payload.response.HolidayDetailsDTO; // Reusing
import com.hrms.hr.payload.request.HolidayManagementRequest;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

// Assuming ResourceNotFoundException is defined (e.g., in HREmployeeService or a common place)
// If not, it should be defined here or imported.
// For this subtask, let's assume it's accessible or we use RuntimeException for simplicity.

// Define custom exceptions or use a common exception package
class ResourceNotFoundException extends RuntimeException { // Re-defined here for scope
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

@Service
public class HRHolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository; // For associating non-global holidays

    @Transactional
    public HolidayDetailsDTO addHoliday(HolidayManagementRequest request, UserDetailsImpl hrUserDetails) {
        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        Holiday holiday = new Holiday();
        holiday.setName(request.getName());
        holiday.setDate(request.getDate());
        holiday.setDescription(request.getDescription());
        holiday.setGlobal(request.getIsGlobal());

        if (!request.getIsGlobal()) {
            Company hrCompany = hrUser.getCompany();
            if (hrCompany == null) {
                // This HR user is not associated with a company, cannot add company-specific holiday
                throw new IllegalStateException("HR user is not associated with a company to add a non-global holiday.");
            }
            holiday.setCompany(hrCompany);
        } else {
            // For global holidays, company is null.
            // Add any specific permission checks for adding global holidays if needed.
            // For now, any HR role can add global ones.
            holiday.setCompany(null);
        }

        Holiday savedHoliday = holidayRepository.save(holiday);
        return mapToHolidayDetailsDTO(savedHoliday);
    }

    @Transactional
    public HolidayDetailsDTO updateHoliday(Long holidayId, HolidayManagementRequest request, UserDetailsImpl hrUserDetails) {
        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        Holiday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with ID: " + holidayId));

        // Permission Check: HR can manage their own company's holidays or global holidays.
        if (!holiday.isGlobal()) { // It's a company-specific holiday
            if (holiday.getCompany() == null || hrUser.getCompany() == null ||
                !Objects.equals(holiday.getCompany().getId(), hrUser.getCompany().getId())) {
                throw new AccessDeniedException("HR is not authorized to update this company-specific holiday.");
            }
        } // Implicitly, HR can manage global holidays (isGlobal=true, company=null)

        holiday.setName(request.getName());
        holiday.setDate(request.getDate());
        holiday.setDescription(request.getDescription());

        // Handling change of isGlobal and company association
        boolean oldIsGlobal = holiday.isGlobal();
        Company oldCompany = holiday.getCompany();

        holiday.setGlobal(request.getIsGlobal());

        if (!request.getIsGlobal()) { // New state is company-specific
            Company targetCompany = hrUser.getCompany();
            if (targetCompany == null) {
                 throw new IllegalStateException("HR user is not associated with a company to set a non-global holiday.");
            }
            holiday.setCompany(targetCompany);
        } else { // New state is global
            holiday.setCompany(null);
        }

        Holiday updatedHoliday = holidayRepository.save(holiday);
        return mapToHolidayDetailsDTO(updatedHoliday);
    }

    @Transactional
    public void deleteHoliday(Long holidayId, UserDetailsImpl hrUserDetails) {
         User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        Holiday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with ID: " + holidayId));

        // Permission Check
         if (!holiday.isGlobal()) { // Company-specific holiday
            if (holiday.getCompany() == null || hrUser.getCompany() == null ||
                !Objects.equals(holiday.getCompany().getId(), hrUser.getCompany().getId())) {
                throw new AccessDeniedException("HR is not authorized to delete this company-specific holiday.");
            }
        } // HR can delete global holidays

        holidayRepository.deleteById(holidayId);
    }

    private HolidayDetailsDTO mapToHolidayDetailsDTO(Holiday holiday) {
        return new HolidayDetailsDTO(
                holiday.getName(),
                holiday.getDate(),
                holiday.getDescription(),
                holiday.isGlobal()
        );
    }
}
