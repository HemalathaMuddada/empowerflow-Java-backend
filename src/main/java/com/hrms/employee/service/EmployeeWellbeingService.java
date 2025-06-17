package com.hrms.employee.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.payload.response.BirthdayGreetingResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.MonthDay;

@Service
public class EmployeeWellbeingService {

    @Value("${hrms.birthday.greeting.default-image-url}")
    private String defaultImageUrl;

    @Value("${hrms.birthday.greeting.default-audio-url}")
    private String defaultAudioUrl;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public BirthdayGreetingResponse getBirthdayGreetingInfo(UserDetailsImpl currentUserDetails) {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        if (user.getDateOfBirth() == null) {
            // User has no date of birth set, so no birthday greeting
            return new BirthdayGreetingResponse(false, null, null, null);
        }

        MonthDay today = MonthDay.now();
        MonthDay userBirthdayMonthDay = MonthDay.from(user.getDateOfBirth());

        if (today.equals(userBirthdayMonthDay)) {
            String greetingMessage = String.format("Happy Birthday, %s! We wish you a fantastic day!", user.getFirstName());
            return new BirthdayGreetingResponse(true, greetingMessage, defaultImageUrl, defaultAudioUrl);
        } else {
            return new BirthdayGreetingResponse(false, null, null, null);
        }
    }
}
