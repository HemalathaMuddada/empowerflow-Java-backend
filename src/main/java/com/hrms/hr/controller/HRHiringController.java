package com.hrms.hr.controller;

import com.hrms.hr.payload.request.HRResumeUploadRequest;
import com.hrms.hr.payload.response.HiringResumeDTO;
import com.hrms.hr.payload.response.HiringResumeListResponse;
import com.hrms.hr.service.HRHiringService;
// Assuming BadRequestException, ResourceNotFoundException are accessible
import com.hrms.hr.service.BadRequestException;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/hr/hiring/resumes")
@PreAuthorize("hasRole('ROLE_HR')")
public class HRHiringController {

    @Autowired
    private HRHiringService hrHiringService;

    @PostMapping("/upload")
    public ResponseEntity<HiringResumeDTO> uploadResumeLink(
            @Valid @RequestBody HRResumeUploadRequest request,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            HiringResumeDTO resumeDTO = hrHiringService.uploadResumeLink(request, hrUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(resumeDTO);
        } catch (BadRequestException ex) { // Assuming BadRequestException is defined and used for validation issues
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading resume link.", ex);
        }
    }

    @GetMapping("/")
    public ResponseEntity<HiringResumeListResponse> listResumes(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            HiringResumeListResponse response = hrHiringService.listResumes(skill, category, hrUser);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error listing resumes.", ex);
        }
    }
}
