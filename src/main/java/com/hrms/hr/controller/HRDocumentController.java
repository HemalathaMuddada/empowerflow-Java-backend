package com.hrms.hr.controller;

import com.hrms.employee.payload.response.DocumentListItemDTO; // Reusing
import com.hrms.hr.payload.request.HRDocumentUploadRequest;
import com.hrms.hr.service.HRDocumentService;
import com.hrms.exception.BadRequestException; // Changed to common
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid; // Already present
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/hr/documents")
@PreAuthorize("hasAnyRole('ROLE_HR', 'ROLE_MANAGER')")
public class HRDocumentController {

    @Autowired
    private HRDocumentService hrDocumentService;

    @PostMapping("/upload-company-wide")
    public ResponseEntity<DocumentListItemDTO> uploadCompanyDocument(
            @Valid @RequestPart("metadata") HRDocumentUploadRequest metadataRequest, // @Valid was already here
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            DocumentListItemDTO documentDto = hrDocumentService.uploadCompanyDocument(metadataRequest, file, hrUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(documentDto);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (com.hrms.exception.ResourceNotFoundException ex) { // If HR user not found
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
        catch (IOException ex) {
            // Log this exception with more detail on the server side
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file.", ex);
        } catch (RuntimeException ex) {
            // Catch-all for other unexpected runtime exceptions
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", ex);
        }
    }
}
