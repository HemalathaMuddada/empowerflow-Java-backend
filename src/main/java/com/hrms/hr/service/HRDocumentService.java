package com.hrms.hr.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.core.repository.CompanyRepository; // Not strictly needed if HR user's company is used
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Document;
import com.hrms.employee.core.enums.DocumentType;
import com.hrms.employee.core.repository.DocumentRepository;
import com.hrms.employee.payload.response.DocumentListItemDTO; // Reusing
import com.hrms.hr.payload.request.HRDocumentUploadRequest;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Assuming ResourceNotFoundException is defined (e.g., in HREmployeeService or a common place)
// If not, it should be defined here or imported.

@Service
public class HRDocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    // CompanyRepository might not be needed if we rely on hrUser.getCompany()
    // @Autowired
    // private CompanyRepository companyRepository;

    @Value("${hrms.company-document.storage.base-path}")
    private String storageBasePath;

    @Transactional
    public DocumentListItemDTO uploadCompanyDocument(HRDocumentUploadRequest request, MultipartFile file, UserDetailsImpl hrUserDetails) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty.");
        }
        // Add more checks: file size, content type if necessary

        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        Company targetCompany = null;
        if (!request.getIsGlobalPolicy()) {
            targetCompany = hrUser.getCompany();
            if (targetCompany == null) {
                throw new IllegalStateException("HR user is not associated with a company to upload a company-specific document.");
            }
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf(".");
        if (extensionIndex > 0 && extensionIndex < originalFilename.length() - 1) {
            fileExtension = originalFilename.substring(extensionIndex);
        }
        String generatedFilename = UUID.randomUUID().toString() + fileExtension;

        Path targetLocation = Paths.get(storageBasePath).resolve(generatedFilename).normalize();
        Files.createDirectories(targetLocation.getParent()); // Ensure directory exists
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(request.getDocumentType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid document type: " + request.getDocumentType());
        }

        Document document = new Document();
        document.setDisplayName(request.getDisplayName());
        document.setDocumentType(documentType);
        document.setDescription(request.getDescription());
        document.setFileName(generatedFilename);
        document.setFileUrl(generatedFilename); // Relative path to be combined with base path for serving
        document.setUploadedBy(hrUser);
        document.setCompanyWide(true); // This endpoint is for company-wide docs
        document.setGlobalPolicy(request.getIsGlobalPolicy());
        document.setCompany(targetCompany); // Null if global policy
        document.setEmployee(null); // Not an employee-specific document

        Document savedDocument = documentRepository.save(document);
        return mapToDocumentListItemDTO(savedDocument, hrUser, targetCompany);
    }

    // This mapper logic is similar to one in employee.service.DocumentService
    // Consider moving to a common utility or mapper class.
    private DocumentListItemDTO mapToDocumentListItemDTO(Document document, User uploader, Company effectiveCompany) {
        String uploaderName = (uploader != null) ? (uploader.getFirstName() + " " + uploader.getLastName()) : "System";

        String scope;
        if (document.isGlobalPolicy()) {
            scope = "Global" + (document.isRestrictedToHR() ? " (HR Restricted)" : "");
        } else if (document.getEmployee() != null) { // Personal document (HR might see this if browsing an employee's file)
             scope = "Personal: " + document.getEmployee().getUsername() + (document.isRestrictedToHR() ? " (HR Restricted)" : "");
        } else if (document.isCompanyWide() && effectiveCompany != null) {
            scope = "Company: " + effectiveCompany.getName() + (document.isRestrictedToHR() ? " (HR Restricted)" : "");
        } else {
            scope = "Context Unknown" + (document.isRestrictedToHR() ? " (HR Restricted)" : "");
        }


        return new DocumentListItemDTO(
                document.getId(),
                document.getDisplayName(),
                document.getDocumentType().name(),
                document.getDescription(),
                document.getCreatedAt(),
                uploaderName,
                scope
        );
    }

    @Transactional(readOnly = true)
    public com.hrms.employee.payload.response.DocumentListResponse listAllCompanyAndGlobalDocumentsForHR(
            UserDetailsImpl hrUserDetails, String filterByTypeString) {
        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        Company hrCompany = hrUser.getCompany();
        Long hrCompanyId = (hrCompany != null) ? hrCompany.getId() : null;

        DocumentType filterByType = null;
        if (StringUtils.hasText(filterByTypeString)) {
            try {
                filterByType = DocumentType.valueOf(filterByTypeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid document type filter: " + filterByTypeString);
            }
        }

        List<Document> companyDocs = hrCompanyId != null ?
            documentRepository.findAllByCompanyIdAndIsCompanyWideTrueAndOptionalDocumentType(hrCompanyId, filterByType) : List.of();

        List<Document> globalDocs = documentRepository.findAllByIsGlobalPolicyTrueAndOptionalDocumentType(filterByType);

        // Note: This method currently does not fetch employee-specific documents for HR.
        // A more comprehensive HR document view might need to list documents for specific employees too.

        List<DocumentListItemDTO> allDocs = Stream.of(companyDocs, globalDocs)
                .flatMap(List::stream)
                .distinct()
                .map(doc -> mapToDocumentListItemDTO(doc, doc.getUploadedBy(), doc.getCompany() != null ? doc.getCompany() : hrCompany))
                .sorted(Comparator.comparing(DocumentListItemDTO::getUploadedAt).reversed())
                .collect(Collectors.toList());

        return new com.hrms.employee.payload.response.DocumentListResponse(allDocs);
    }
}
