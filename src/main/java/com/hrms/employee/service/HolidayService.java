package com.hrms.employee.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Holiday;
import com.hrms.employee.core.repository.HolidayRepository;
import com.hrms.employee.payload.response.HolidayDetailsDTO;
import com.hrms.employee.payload.response.HolidayListResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public HolidayListResponse getApplicableHolidays(UserDetailsImpl currentUserDetails) {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        Company company = user.getCompany();
        Long companyId = (company != null) ? company.getId() : null;

        List<Holiday> companyHolidays = companyId != null ? holidayRepository.findByCompanyId(companyId) : List.of();
        List<Holiday> globalHolidays = holidayRepository.findByIsGlobalTrue();

        List<HolidayDetailsDTO> combinedHolidays = Stream.concat(companyHolidays.stream(), globalHolidays.stream())
                .distinct() // Avoid duplicates if a global holiday is also listed under company
                .map(this::mapToHolidayDetailsDTO)
                .sorted(Comparator.comparing(HolidayDetailsDTO::getDate))
                .collect(Collectors.toList());

        return new HolidayListResponse(combinedHolidays);
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
