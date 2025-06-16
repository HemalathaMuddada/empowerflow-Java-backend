package com.hrms.employee.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Attendance;
import com.hrms.employee.core.repository.AttendanceRepository;
import com.hrms.employee.payload.request.AttendanceLogRequest;
import com.hrms.employee.payload.response.AttendanceRecordDTO;
import com.hrms.employee.payload.response.AttendanceSummaryResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public AttendanceRecordDTO logOrUpdateAttendance(AttendanceLogRequest request, UserDetailsImpl currentUserDetails) {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        Optional<Attendance> existingAttendanceOpt = attendanceRepository.findByEmployeeAndWorkDate(user, request.getWorkDate());

        Attendance attendance;
        if (existingAttendanceOpt.isPresent()) {
            attendance = existingAttendanceOpt.get();
        } else {
            attendance = new Attendance();
            attendance.setEmployee(user);
            attendance.setWorkDate(request.getWorkDate());
        }

        LocalDateTime loginDateTime = LocalDateTime.of(request.getWorkDate(), request.getLoginTime());
        attendance.setLoginTime(loginDateTime);

        if (request.getLogoutTime() != null) {
            LocalDateTime logoutDateTime = LocalDateTime.of(request.getWorkDate(), request.getLogoutTime());
            // Ensure logout is not before login on the same day
            if (logoutDateTime.isBefore(loginDateTime)) {
                throw new IllegalArgumentException("Logout time cannot be before login time on the same day.");
            }
            attendance.setLogoutTime(logoutDateTime);
            attendance.setTotalHours(calculateWorkHours(loginDateTime, logoutDateTime));
        } else {
            // If logout time is explicitly cleared, reset totalHours
            attendance.setLogoutTime(null);
            attendance.setTotalHours(null);
        }
        // isRegularized and notes would be handled by a separate regularization process

        Attendance savedAttendance = attendanceRepository.save(attendance);
        return mapToAttendanceRecordDTO(savedAttendance);
    }

    private Double calculateWorkHours(LocalDateTime login, LocalDateTime logout) {
        if (login == null || logout == null || logout.isBefore(login)) {
            return null; // Or 0.0, depending on policy for invalid data
        }
        Duration duration = Duration.between(login, logout);
        // Using BigDecimal for precision might be better for financial calculations, but double is fine for hours.
        return duration.toMinutes() / 60.0;
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceForPeriod(LocalDate startDate, LocalDate endDate, UserDetailsImpl currentUserDetails) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        List<Attendance> records = attendanceRepository.findByEmployeeAndWorkDateBetweenOrderByWorkDateAsc(user, startDate, endDate);
        List<AttendanceRecordDTO> dtoList = records.stream()
                .map(this::mapToAttendanceRecordDTO)
                .collect(Collectors.toList());
        return new AttendanceSummaryResponse(dtoList);
    }

    private AttendanceRecordDTO mapToAttendanceRecordDTO(Attendance attendance) {
        return new AttendanceRecordDTO(
                attendance.getId(),
                attendance.getWorkDate(),
                attendance.getLoginTime(),
                attendance.getLogoutTime(),
                attendance.getTotalHours(),
                attendance.isRegularized(),
                attendance.getNotes()
        );
    }
}
