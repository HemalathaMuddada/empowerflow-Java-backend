package com.hrms.employee.controller;

import com.hrms.employee.payload.response.DocumentListResponse;
import com.hrms.employee.service.DocumentService;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee/documents")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping("/")
    public ResponseEntity<DocumentListResponse> listDocuments(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) String type) {
        DocumentListResponse response = documentService.listAccessibleDocuments(currentUser, type);
        return ResponseEntity.ok(response);
    }
}
