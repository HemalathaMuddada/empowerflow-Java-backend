package com.hrms.lead.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamRegularizationRequestDTO {
    private Long regularizationRequestId;
    private Long employeeId;
    private String employeeName;
    private LocalDate requestDate; // Date for which regularization is requested
    private String reasonType;     // From RegularizationReason enum
    private String customReason;   // Nullable
    private String status;         // From RegularizationStatus enum
    private LocalDateTime createdAt;    // When the request was submitted

    // Optional: fields from linked Attendance record
    private LocalDate attendanceWorkDate; // Nullable
    private LocalDateTime attendanceLoginTime; // Nullable
    private LocalDateTime attendanceLogoutTime; // Nullable
}
