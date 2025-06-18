package com.hrms.employee.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Document;
import com.hrms.employee.core.entity.InvestmentDeclaration;
import com.hrms.employee.core.enums.DocumentType;
import com.hrms.employee.core.repository.DocumentRepository;
import com.hrms.employee.core.repository.InvestmentDeclarationRepository;
import com.hrms.employee.payload.request.InvestmentDeclarationRequest;
import com.hrms.employee.payload.response.InvestmentDeclarationDTO;
import com.hrms.exception.BadRequestException; // Changed to common
import com.hrms.exception.ResourceNotFoundException; // Changed to common
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class EmployeeInvestmentService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeInvestmentService.class);

    @Autowired
    private InvestmentDeclarationRepository investmentDeclarationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Value("${hrms.investment-proof.storage.base-path:/tmp/hrms/investment-proofs/}")
    private String storageBasePath;

    @Transactional
    public InvestmentDeclarationDTO submitDeclaration(
            InvestmentDeclarationRequest request,
            MultipartFile itDocumentFile,
            UserDetailsImpl employeeUserDetails) throws IOException {

        User employee = userRepository.findById(employeeUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeUserDetails.getId()));

        // Check for existing declaration for the same year by this employee
        if(investmentDeclarationRepository.findByEmployee_IdAndDeclarationYear(employee.getId(), request.getDeclarationYear()).size() > 0) {
            throw new DataIntegrityViolationException(
                String.format("An investment declaration for the year %s already exists for employee %s.",
                request.getDeclarationYear(), employee.getUsername()));
        }

        String documentUrl = null;
        if (itDocumentFile != null && !itDocumentFile.isEmpty()) {
            if (itDocumentFile.getSize() > 5 * 1024 * 1024) { // 5MB limit
                throw new BadRequestException("Proof document size exceeds 5MB limit.");
            }
            // Basic content type check (optional, can be more specific)
            // if (!List.of("application/pdf", "image/jpeg", "image/png").contains(itDocumentFile.getContentType())) {
            //     throw new BadRequestException("Invalid file type. Only PDF, JPG, PNG allowed.");
            // }

            String originalFilename = StringUtils.cleanPath(itDocumentFile.getOriginalFilename());
            String fileExtension = "";
            int extensionIndex = originalFilename.lastIndexOf(".");
            if (extensionIndex > 0 && extensionIndex < originalFilename.length() - 1) {
                fileExtension = originalFilename.substring(extensionIndex);
            }
            String generatedFilename = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = Paths.get(storageBasePath).resolve(generatedFilename).normalize();
            Files.createDirectories(targetLocation.getParent());
            Files.copy(itDocumentFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            documentUrl = generatedFilename; // Store relative path or full, based on serving strategy

            // Create Document entity for the proof
            Document proofDocument = new Document();
            proofDocument.setEmployee(employee);
            proofDocument.setDocumentType(DocumentType.INVESTMENT_PROOF);
            proofDocument.setDisplayName("IT Declaration Proof " + request.getDeclarationYear());
            proofDocument.setFileName(generatedFilename);
            proofDocument.setFileUrl(documentUrl); // Relative path
            proofDocument.setUploadedBy(employee);
            proofDocument.setCompanyWide(false);
            proofDocument.setGlobalPolicy(false);
            proofDocument.setRestrictedToHR(true); // Investment proofs are sensitive
            documentRepository.save(proofDocument);
        }

        InvestmentDeclaration declaration = new InvestmentDeclaration();
        declaration.setEmployee(employee);
        declaration.setDeclarationYear(request.getDeclarationYear());
        declaration.setItDeclarationAmount(request.getItDeclarationAmount());
        declaration.setFbpOptedAmount(request.getFbpOptedAmount());
        declaration.setFbpChoicesJson(request.getFbpChoicesJson());
        declaration.setItDocumentUrl(documentUrl); // Link to the Document's fileUrl or ID
        declaration.setStatus("SUBMITTED"); // Initial status
        // submittedAt and updatedAt are handled by JPA auditing

        InvestmentDeclaration savedDeclaration = investmentDeclarationRepository.save(declaration);
        return mapToInvestmentDeclarationDTO(savedDeclaration);
    }

    private InvestmentDeclarationDTO mapToInvestmentDeclarationDTO(InvestmentDeclaration entity) {
        String reviewedByName = null;
        if (entity.getReviewedBy() != null) {
            reviewedByName = entity.getReviewedBy().getFirstName() + " " + entity.getReviewedBy().getLastName();
        }
        String employeeName = entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName();

        return new InvestmentDeclarationDTO(
                entity.getId(),
                entity.getEmployee().getId(),
                employeeName,
                entity.getDeclarationYear(),
                entity.getItDeclarationAmount(),
                entity.getFbpOptedAmount(),
                entity.getItDocumentUrl(),
                entity.getFbpChoicesJson(),
                entity.getStatus(),
                entity.getSubmittedAt(),
                reviewedByName,
                entity.getReviewedAt(),
                entity.getHrComments(),
                entity.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<InvestmentDeclarationDTO> getMyDeclarations(UserDetailsImpl employeeUserDetails) {
        User employee = userRepository.findById(employeeUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeUserDetails.getId()));

        List<InvestmentDeclaration> declarations = investmentDeclarationRepository.findByEmployeeOrderByDeclarationYearDescSubmittedAtDesc(employee);

        return declarations.stream()
                           .map(this::mapToInvestmentDeclarationDTO)
                           .collect(Collectors.toList());
    }
}
