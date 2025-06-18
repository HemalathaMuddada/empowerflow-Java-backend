package com.hrms.hr.service;

import com.hrms.audit.service.AuditLogService;
import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.core.repository.CompanyRepository;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.InvestmentDeclaration;
import com.hrms.employee.core.repository.InvestmentDeclarationRepository;
import com.hrms.employee.payload.response.InvestmentDeclarationDTO;
import com.hrms.hr.payload.request.HRDeclarationActionRequest;
import com.hrms.hr.specs.InvestmentDeclarationSpecification;
import com.hrms.security.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// Assuming ResourceNotFoundException & BadRequestException are defined (e.g., locally or in a common package)
// Re-defining for clarity within this service's context if not using common ones.
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}


@Service
public class HRInvestmentService {
    private static final Logger logger = LoggerFactory.getLogger(HRInvestmentService.class);

    @Autowired
    private InvestmentDeclarationRepository investmentDeclarationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository; // To validate companyId if provided

    @Autowired
    private AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<InvestmentDeclarationDTO> getDeclarationsForCompany(
            Long filterCompanyId, String statusFilter, UserDetailsImpl hrUserDetails, Pageable pageable) {

        User hrUser = userRepository.findById(hrUserDetails.getId())
            .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        Long effectiveCompanyId = filterCompanyId;
        if (hrUser.getCompany() != null) {
            if (filterCompanyId == null) {
                effectiveCompanyId = hrUser.getCompany().getId();
            } else if (!Objects.equals(hrUser.getCompany().getId(), filterCompanyId)) {
                throw new AccessDeniedException("HR users can only view declarations for their own company or a specified valid company ID if global.");
            }
        } else { 
            if (filterCompanyId == null) {
                 throw new BadRequestException("Global HR must specify a companyId to filter declarations.");
            }
        }

        if (effectiveCompanyId != null) {
            companyRepository.findById(effectiveCompanyId).orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + effectiveCompanyId));
        }


        Specification<InvestmentDeclaration> spec = InvestmentDeclarationSpecification.filterDeclarations(effectiveCompanyId, statusFilter);
        Page<InvestmentDeclaration> declarationPage = investmentDeclarationRepository.findAll(spec, pageable);

        List<InvestmentDeclarationDTO> dtoList = declarationPage.getContent().stream()
                .map(this::mapToInvestmentDeclarationDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, declarationPage.getTotalElements());
    }

    @Transactional
    public InvestmentDeclarationDTO approveOrRejectDeclaration(
            Long declarationId, HRDeclarationActionRequest request, UserDetailsImpl hrUserDetails) {

        InvestmentDeclaration decl = investmentDeclarationRepository.findById(declarationId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment Declaration not found with ID: " + declarationId));

        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        // Permission Check: HR can action declarations for employees in their company, or any if global HR.
        Company employeeCompany = decl.getEmployee().getCompany();
        if (hrUser.getCompany() != null) { // HR is tied to a company
            if (employeeCompany == null || !Objects.equals(hrUser.getCompany().getId(), employeeCompany.getId())) {
                throw new AccessDeniedException("HR user cannot action declarations for employees outside their own company.");
            }
        } // If hrUser.getCompany() is null, they are global HR and can action any.

        if (!"SUBMITTED".equalsIgnoreCase(decl.getStatus())) {
            throw new IllegalStateException("Declaration is not in SUBMITTED status, cannot be actioned. Current status: " + decl.getStatus());
        }

        String action = request.getAction().toUpperCase();
        String oldStatus = decl.getStatus();

        if ("APPROVE".equals(action)) {
            decl.setStatus("APPROVED_BY_HR");
        } else if ("REJECT".equals(action)) {
            decl.setStatus("REJECTED_BY_HR");
        } else {
            // Should be caught by DTO validation, but good to have a safeguard
            throw new BadRequestException("Invalid action: " + request.getAction());
        }

        decl.setReviewedBy(hrUser);
        decl.setReviewedAt(LocalDateTime.now());
        decl.setHrComments(request.getHrComments());

        InvestmentDeclaration updatedDecl = investmentDeclarationRepository.save(decl);

        String auditActionType = "APPROVE".equals(action) ? "INVESTMENT_DECLARATION_APPROVED" : "INVESTMENT_DECLARATION_REJECTED";
        String auditDetails = String.format("Declaration ID %d for employee %s (%s) %s. Comments: %s",
                                            updatedDecl.getId(), updatedDecl.getEmployee().getUsername(),
                                            updatedDecl.getDeclarationYear(), action.toLowerCase(),
                                            request.getHrComments() != null ? request.getHrComments() : "N/A");
        auditLogService.logEvent(
            hrUserDetails.getUsername(),
            hrUserDetails.getId(),
            auditActionType,
            "InvestmentDeclaration",
            String.valueOf(updatedDecl.getId()),
            auditDetails,
            null, "SUCCESS"
        );

        return mapToInvestmentDeclarationDTO(updatedDecl);
    }

    private InvestmentDeclarationDTO mapToInvestmentDeclarationDTO(InvestmentDeclaration entity) {
        String reviewedByName = (entity.getReviewedBy() != null) ?
                                (entity.getReviewedBy().getFirstName() + " " + entity.getReviewedBy().getLastName()) : null;
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
}
