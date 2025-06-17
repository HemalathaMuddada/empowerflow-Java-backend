package com.hrms.hr.controller;

import com.hrms.employee.payload.response.HikeDetailDTO; // Reusing
import com.hrms.hr.payload.request.HRManualHikeRecordRequest;
import com.hrms.hr.payload.request.PublishHikesRequest; // New DTO
import com.hrms.hr.payload.response.HikeCsvUploadSummaryDTO;
import com.hrms.hr.payload.response.HikePublishSummaryDTO; // New DTO
import com.hrms.hr.service.HRHikeService;
import com.hrms.hr.service.BadRequestException;
import com.hrms.hr.service.ResourceNotFoundException;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List; // Required for List.of in CSV error handling

@RestController
@RequestMapping("/api/hr/hikes")
@PreAuthorize("hasRole('ROLE_HR')")
public class HRHikeController {

    @Autowired
    private HRHikeService hrHikeService;

    @PostMapping("/manual-add")
    public ResponseEntity<HikeDetailDTO> addManualHikeRecord(
            @Valid @RequestBody HRManualHikeRecordRequest request,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            HikeDetailDTO hikeDetail = hrHikeService.addHikeRecordManually(request, hrUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(hikeDetail);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (BadRequestException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while adding hike record.", ex);
        }
    }

    @PostMapping("/upload-csv")
    public ResponseEntity<HikeCsvUploadSummaryDTO> uploadHikeRecordsCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new HikeCsvUploadSummaryDTO(0,0,0, List.of("Upload file cannot be empty.")));
        }
        try {
            HikeCsvUploadSummaryDTO summary = hrHikeService.processHikeCsvUpload(file, hrUser);
            if (summary.getFailedRecords() > 0) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(summary);
            }
            return ResponseEntity.ok(summary);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HikeCsvUploadSummaryDTO(0,0,0, List.of("Failed to process CSV file: " + ex.getMessage())));
        } catch (Exception ex) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HikeCsvUploadSummaryDTO(0,0,0, List.of("An unexpected error occurred: " + ex.getMessage())));
        }
    }

    @PostMapping("/publish")
    public ResponseEntity<HikePublishSummaryDTO> publishHikeRecords(
            @Valid @RequestBody PublishHikesRequest request,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            HikePublishSummaryDTO summary = hrHikeService.publishHikeRecords(request, hrUser);
            // Determine overall status based on summary for more granular response
            if (summary.getFailedRecords() > 0 ||
                summary.getNotFoundSkippedCount() > 0 ||
                summary.getPermissionDeniedSkippedCount() > 0 ||
                summary.getEmailFailedCount() > (summary.getSuccessfullyPublishedCount() - summary.getEmailSentCount())) { // if some emails failed for successfully published items

                if (summary.getSuccessfullyPublishedCount() > 0 || summary.getEmailSentCount() > 0) {
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(summary); // Partial success
                } else {
                     // All requested items failed or were skipped for reasons other than "already published"
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(summary);
                }
            }
            if(summary.getTotalRequested() == summary.getAlreadyPublishedSkippedCount()){
                 return ResponseEntity.status(HttpStatus.OK).body(summary); // OK, but nothing new was published
            }
            return ResponseEntity.ok(summary); // All successfully published and emails sent (or mailer not configured)
        } catch (Exception ex) {
             // Log ex
             HikePublishSummaryDTO errorSummary = new HikePublishSummaryDTO(request.getHikeRecordIds() != null ? request.getHikeRecordIds().size() : 0);
             errorSummary.addDetail("An unexpected server error occurred during publishing: " + ex.getMessage());
             // Populate counts to reflect total failure if possible
             errorSummary.setFailedRecords(errorSummary.getTotalRequested());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorSummary);
        }
    }
}
