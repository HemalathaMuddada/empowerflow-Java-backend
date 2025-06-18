package com.hrms.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HiringResumeDTO {
    private Long id;
    private String candidateName;
    private String resumeLink;
    private String skills;
    private String category;
    private String notes;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
}
