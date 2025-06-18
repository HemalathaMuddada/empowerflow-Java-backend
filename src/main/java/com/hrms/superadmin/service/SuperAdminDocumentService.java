package com.hrms.superadmin.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository; // For uploader name
import com.hrms.employee.core.entity.Document;
import com.hrms.employee.core.repository.DocumentRepository;
import com.hrms.employee.payload.response.DocumentListItemDTO; // Reusing
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Assuming ResourceNotFoundException is defined elsewhere or not strictly needed for this read-only service
// class ResourceNotFoundException extends RuntimeException { public ResourceNotFoundException(String message) { super(message); } }


@Service
public class SuperAdminDocumentService {
    private static final Logger logger = LoggerFactory.getLogger(SuperAdminDocumentService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository; // Required for fetching uploader details if not eager loaded

    private DocumentListItemDTO mapToDocumentListItemDTO(Document document) {
        String uploaderName = "System"; // Default if no uploader or uploader not found
        if (document.getUploadedBy() != null) {
            // Assuming getUploadedBy() returns a User proxy or eagerly fetched User.
            // If it's just an ID, would need to fetch from UserRepository.
            // For now, assume User object is available or username is on User proxy.
            User uploader = document.getUploadedBy(); // This might be a proxy
            // To be safe, fetch the user if only ID is guaranteed.
            // For this example, we'll assume getUsername() is available on the proxy or it's fetched.
            // User uploader = userRepository.findById(document.getUploadedBy().getId()).orElse(null);
            // if (uploader != null) { uploaderName = uploader.getUsername(); }
            uploaderName = uploader.getUsername(); // Or getFirstName + getLastName
        }

        String scope = "Unknown";
        if (document.isGlobalPolicy()) {
            scope = "Global Policy";
        } else if (document.isCompanyWide() && document.getCompany() != null) {
            scope = "Company: " + document.getCompany().getName();
        } else if (document.getEmployee() != null) {
            scope = "Personal: " + document.getEmployee().getUsername();
        }
        if (document.isRestrictedToHR()) {
            scope += " (HR Restricted)";
        }


        return new DocumentListItemDTO(
                document.getId(),
                document.getDisplayName(),
                document.getDocumentType().name(),
                document.getDescription(),
                document.getCreatedAt(), // Assuming createdAt is the 'uploadedAt' timestamp
                uploaderName,
                scope
        );
    }

    @Transactional(readOnly = true)
    public Page<DocumentListItemDTO> getAllGlobalPolicyDocuments(Pageable pageable, UserDetailsImpl superAdminUser) {
        // No specific permission check beyond ROLE_SUPER_ADMIN for viewing global policies.
        logger.info("SuperAdmin {} fetching all global policy documents, page: {}, size: {}",
            superAdminUser.getUsername(), pageable.getPageNumber(), pageable.getPageSize());

        Page<Document> globalPolicyDocumentsPage = documentRepository.findByIsGlobalPolicyTrue(pageable);

        List<DocumentListItemDTO> dtoList = globalPolicyDocumentsPage.getContent().stream()
                .map(this::mapToDocumentListItemDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, globalPolicyDocumentsPage.getTotalElements());
    }
}
