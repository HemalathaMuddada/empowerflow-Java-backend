package com.hrms.hr.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRResumeUploadRequest {

    @NotBlank(message = "Candidate name cannot be blank")
    @Size(max = 255)
    private String candidateName;

    @NotBlank(message = "Resume link cannot be blank")
    @URL(message = "Resume link must be a valid URL")
    @Size(max = 2048)
    private String resumeLink;

    @NotBlank(message = "Skills cannot be blank")
    private String skills; // Comma-separated or simple text

    @NotBlank(message = "Category cannot be blank")
    @Size(max = 255)
    private String category;

    private String notes; // Nullable
}
