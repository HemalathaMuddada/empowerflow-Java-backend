package com.hrms.manager.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Document;
import com.hrms.employee.core.enums.DocumentType;
import com.hrms.employee.core.repository.DocumentRepository;
import com.hrms.employee.payload.response.DocumentListItemDTO; // Reusing
import com.hrms.manager.payload.request.ManagerHikeDocumentUploadRequest;
import com.hrms.exception.ResourceNotFoundException;  // Changed to common
import com.hrms.exception.BadRequestException;      // Changed to common
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
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
import java.util.Objects;
import java.util.UUID;

@Service
public class ManagerDocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${hrms.company-document.storage.base-path}") // Using this, or a more general one if available
    private String storageBasePath;

    @Transactional
    public DocumentListItemDTO uploadHikeDocumentForEmployee(
            ManagerHikeDocumentUploadRequest request,
            MultipartFile file,
            UserDetailsImpl managerUserDetails) throws IOException {

        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty.");
        }
        // Add more file validation if needed (size, content type)

        User managerUser = userRepository.findById(managerUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Manager user not found: " + managerUserDetails.getUsername()));

        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Target employee not found with ID: " + request.getEmployeeId()));

        Company managerCompany = managerUser.getCompany();
        if (managerCompany == null) {
            throw new IllegalStateException("Manager " + managerUser.getUsername() + " is not associated with any company.");
        }

        // Permission Check: Ensure manager and employee are in the same company.
        // And optionally, that employee reports to this manager (more complex check, skipped for now as per instruction)
        if (employee.getCompany() == null || !Objects.equals(employee.getCompany().getId(), managerCompany.getId())) {
            throw new AccessDeniedException("Manager and Employee are not in the same company or employee is not assigned to a company.");
        }
        // A stricter check would be: employee.getManager() != null && employee.getManager().getId().equals(managerUser.getId())
        // For now, same company check is a basic level of authorization.

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf(".");
        if (extensionIndex > 0 && extensionIndex < originalFilename.length() - 1) {
            fileExtension = originalFilename.substring(extensionIndex);
        }
        String generatedFilename = UUID.randomUUID().toString() + fileExtension;

        Path targetLocation = Paths.get(storageBasePath).resolve(generatedFilename).normalize();
        Files.createDirectories(targetLocation.getParent());
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        Document document = new Document();
        document.setDisplayName(request.getDisplayName());
        document.setDocumentType(DocumentType.HIKE_DOCUMENT); // Specific type
        document.setDescription(request.getDescription());
        document.setFileName(generatedFilename);
        document.setFileUrl(generatedFilename); // Relative path
        document.setUploadedBy(managerUser);
        document.setCompany(managerCompany); // Document associated with manager's company
        document.setEmployee(employee);      // Document associated with the specific employee

        document.setRestrictedToHR(true);    // Key requirement for this feature
        document.setCompanyWide(false);      // Not company-wide in the general sense, but for an employee
        document.setGlobalPolicy(false);

        Document savedDocument = documentRepository.save(document);
        return mapToDocumentListItemDTO(savedDocument);
    }

    // This mapper logic is similar to one in employee.service.DocumentService & hr.service.HRDocumentService
    // Consider moving to a common utility or mapper class.
    private DocumentListItemDTO mapToDocumentListItemDTO(Document document) {
        String uploaderName = (document.getUploadedBy() != null) ?
                (document.getUploadedBy().getFirstName() + " " + document.getUploadedBy().getLastName()) : "System";

        String scope;
        if (document.isGlobalPolicy()) {
            scope = "Global";
        } else if (document.getEmployee() != null) {
            scope = "Personal: " + document.getEmployee().getUsername() + (document.isRestrictedToHR() ? " (HR Restricted)" : "");
        } else if (document.isCompanyWide() && document.getCompany() != null) {
            scope = "Company: " + document.getCompany().getName() + (document.isRestrictedToHR() ? " (HR Restricted)" : "");
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
}
