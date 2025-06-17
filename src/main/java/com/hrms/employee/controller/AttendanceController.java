package com.hrms.employee.controller;

import com.hrms.employee.payload.request.AttendanceLogRequest;
import com.hrms.employee.payload.response.AttendanceRecordDTO;
import com.hrms.employee.payload.response.AttendanceSummaryResponse;
import com.hrms.employee.service.AttendanceService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/employee/attendance")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/log")
    public ResponseEntity<AttendanceRecordDTO> logAttendance(
            @Valid @RequestBody AttendanceLogRequest logRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        AttendanceRecordDTO recordDTO = attendanceService.logOrUpdateAttendance(logRequest, currentUser);
        return ResponseEntity.ok(recordDTO);
    }

    @GetMapping("/summary")
    public ResponseEntity<AttendanceSummaryResponse> getAttendanceSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        if (startDate.isAfter(endDate)) {
            // Or handle this with a global exception handler for MethodArgumentNotValidException / ConstraintViolationException
            return ResponseEntity.badRequest().build(); // Basic bad request
        }
        AttendanceSummaryResponse summaryResponse = attendanceService.getAttendanceForPeriod(startDate, endDate, currentUser);
        return ResponseEntity.ok(summaryResponse);
    }
}
