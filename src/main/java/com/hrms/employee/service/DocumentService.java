package com.hrms.employee.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Document;
import com.hrms.employee.core.enums.DocumentType;
import com.hrms.employee.core.repository.DocumentRepository;
import com.hrms.employee.payload.response.DocumentListItemDTO;
import com.hrms.employee.payload.response.DocumentListResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public DocumentListResponse listAccessibleDocuments(UserDetailsImpl currentUserDetails, String filterByTypeString) {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        Company company = user.getCompany();
        Long companyId = (company != null) ? company.getId() : null;

        DocumentType filterByType = null;
        if (StringUtils.hasText(filterByTypeString)) {
            try {
                filterByType = DocumentType.valueOf(filterByTypeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Handle invalid type string, e.g., log it or throw a specific exception
                // For now, we'll proceed as if no filter was applied or throw an error
                throw new IllegalArgumentException("Invalid document type filter: " + filterByTypeString);
            }
        }

        List<Document> personalDocuments = documentRepository.findByEmployeeIdAndOptionalDocumentTypeAndIsRestrictedToHRFalse(user.getId(), filterByType);
        List<Document> companyDocuments = companyId != null ? documentRepository.findByCompanyIdAndIsCompanyWideTrueAndOptionalDocumentTypeAndIsRestrictedToHRFalse(companyId, filterByType) : List.of();
        List<Document> globalDocuments = documentRepository.findByIsGlobalPolicyTrueAndOptionalDocumentTypeAndIsRestrictedToHRFalse(filterByType);

        List<DocumentListItemDTO> accessibleDocuments = Stream.of(personalDocuments, companyDocuments, globalDocuments)
                .flatMap(List::stream)
                .distinct() // Based on Document's equals/hashCode, assuming it's based on ID.
                .map(doc -> mapToDocumentListItemDTO(doc, user.getId(), companyId))
                .sorted(Comparator.comparing(DocumentListItemDTO::getUploadedAt).reversed())
                .collect(Collectors.toList());

        return new DocumentListResponse(accessibleDocuments);
    }

    private DocumentListItemDTO mapToDocumentListItemDTO(Document document, Long currentUserId, Long currentUserCompanyId) {
        String uploaderName = (document.getUploadedBy() != null) ?
                (document.getUploadedBy().getFirstName() + " " + document.getUploadedBy().getLastName()) : "System";

        String scope = "Global"; // Default for global policy
        if (document.getEmployee() != null && document.getEmployee().getId().equals(currentUserId)) {
            scope = "Personal";
        } else if (document.getCompany() != null && document.getCompany().getId().equals(currentUserCompanyId) && document.isCompanyWide()) {
            scope = "Company";
        }
        // If it's personal, scope is Personal. If it's company-wide for user's company, scope is Company.
        // If it's global, scope is Global. There might be overlaps that distinct() handles.
        // If a document is both company-wide and global, this logic might need refinement based on desired precedence for scope string.
        // For now, "Personal" takes highest precedence.

        return new DocumentListItemDTO(
                document.getId(),
                document.getDisplayName(),
                document.getDocumentType().name(),
                document.getDescription(),
                document.getCreatedAt(), // Assuming createdAt is the uploadedAt timestamp
                uploaderName,
                scope
        );
    }
}
