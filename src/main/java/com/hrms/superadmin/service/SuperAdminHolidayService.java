package com.hrms.superadmin.service;

import com.hrms.audit.service.AuditLogService;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Holiday;
import com.hrms.employee.core.repository.HolidayRepository;
import com.hrms.employee.payload.response.HolidayDetailsDTO; // Reusing
import com.hrms.hr.payload.request.HolidayManagementRequest;   // Reusing
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// Local exception for this service
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

@Service
public class SuperAdminHolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserRepository userRepository; // To fetch User entity for audit logging

    private HolidayDetailsDTO mapToHolidayDetailsDTO(Holiday holiday) {
        return new HolidayDetailsDTO(
                holiday.getName(),
                holiday.getDate(),
                holiday.getDescription(),
                holiday.isGlobal()
        );
    }

    @Transactional
    public HolidayDetailsDTO createGlobalHoliday(HolidayManagementRequest request, UserDetailsImpl superAdminUserDetails) {
        User superAdmin = userRepository.findById(superAdminUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("SuperAdmin user not found"));

        Holiday holiday = new Holiday();
        holiday.setName(request.getName());
        holiday.setDate(request.getDate());
        holiday.setDescription(request.getDescription());
        holiday.setGlobal(true); // Force global
        holiday.setCompany(null); // Ensure no company is associated

        Holiday savedHoliday = holidayRepository.save(holiday);

        auditLogService.logEvent(
                superAdminUserDetails.getUsername(),
                superAdminUserDetails.getId(),
                "GLOBAL_HOLIDAY_CREATE",
                "Holiday",
                String.valueOf(savedHoliday.getId()),
                String.format("Global holiday '%s' for date %s created.", savedHoliday.getName(), savedHoliday.getDate()),
                null, "SUCCESS"
        );
        return mapToHolidayDetailsDTO(savedHoliday);
    }

    @Transactional
    public HolidayDetailsDTO updateGlobalHoliday(Long holidayId, HolidayManagementRequest request, UserDetailsImpl superAdminUserDetails) {
        User superAdmin = userRepository.findById(superAdminUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("SuperAdmin user not found"));

        Holiday holiday = holidayRepository.findByIdAndIsGlobalTrue(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Global holiday not found with ID: " + holidayId + " or it's not a global holiday."));

        String oldDetails = String.format("Name: %s, Date: %s, Desc: %s", holiday.getName(), holiday.getDate(), holiday.getDescription());

        holiday.setName(request.getName());
        holiday.setDate(request.getDate());
        holiday.setDescription(request.getDescription());
        holiday.setGlobal(true); // Ensure it remains global
        holiday.setCompany(null);

        Holiday updatedHoliday = holidayRepository.save(holiday);

        String newDetails = String.format("Name: %s, Date: %s, Desc: %s", updatedHoliday.getName(), updatedHoliday.getDate(), updatedHoliday.getDescription());
        auditLogService.logEvent(
                superAdminUserDetails.getUsername(),
                superAdminUserDetails.getId(),
                "GLOBAL_HOLIDAY_UPDATE",
                "Holiday",
                String.valueOf(updatedHoliday.getId()),
                String.format("Global holiday ID %d updated. Old: [%s], New: [%s]", holidayId, oldDetails, newDetails),
                null, "SUCCESS"
        );
        return mapToHolidayDetailsDTO(updatedHoliday);
    }

    @Transactional
    public void deleteGlobalHoliday(Long holidayId, UserDetailsImpl superAdminUserDetails) {
        Holiday holiday = holidayRepository.findByIdAndIsGlobalTrue(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Global holiday not found with ID: " + holidayId + " or it's not a global holiday."));

        String deletedDetails = String.format("Global holiday '%s' (ID: %d) for date %s deleted.", holiday.getName(), holiday.getId(), holiday.getDate());

        holidayRepository.delete(holiday); // Use delete(entity) to allow for any cascades if defined, or deleteById(id)

        auditLogService.logEvent(
                superAdminUserDetails.getUsername(),
                superAdminUserDetails.getId(),
                "GLOBAL_HOLIDAY_DELETE",
                "Holiday",
                String.valueOf(holidayId),
                deletedDetails,
                null, "SUCCESS"
        );
    }

    @Transactional(readOnly = true)
    public List<HolidayDetailsDTO> getAllGlobalHolidays() {
        List<Holiday> holidays = holidayRepository.findByIsGlobalTrue();
        return holidays.stream()
                .map(this::mapToHolidayDetailsDTO)
                .collect(Collectors.toList());
    }
}
