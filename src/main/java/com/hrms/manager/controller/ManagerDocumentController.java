package com.hrms.manager.controller;

import com.hrms.employee.payload.response.DocumentListItemDTO; // Reusing
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.manager.payload.request.ManagerHikeDocumentUploadRequest;
import com.hrms.manager.service.ManagerDocumentService;
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

@RestController
@RequestMapping("/api/manager/documents")
@PreAuthorize("hasRole('ROLE_MANAGER')")
public class ManagerDocumentController {

    @Autowired
    private ManagerDocumentService managerDocumentService;

    @PostMapping("/upload-hike-document")
    public ResponseEntity<DocumentListItemDTO> uploadHikeDocument(
            @Valid @RequestPart("metadata") ManagerHikeDocumentUploadRequest metadataRequest,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl managerUser) {
        try {
            DocumentListItemDTO documentDto = managerDocumentService.uploadHikeDocumentForEmployee(metadataRequest, file, managerUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(documentDto);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file.", ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", ex);
        }
    }
}
