package com.hrms.superadmin.service;

import com.hrms.core.entity.Company;
import com.hrms.core.repository.CompanyRepository;
import com.hrms.superadmin.payload.request.CompanyCreateRequest;
import com.hrms.superadmin.payload.request.CompanyUpdateRequest;
import com.hrms.superadmin.payload.response.CompanyResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.hrms.exception.ResourceNotFoundException; // Added common exception


@Service
public class SuperAdminCompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Transactional
    public CompanyResponseDTO createCompany(CompanyCreateRequest request) {
        if (companyRepository.findByName(request.getName()).isPresent()) {
            throw new DataIntegrityViolationException("Company with name '" + request.getName() + "' already exists.");
        }
        Company company = new Company();
        company.setName(request.getName());
        company.setAddress(request.getAddress());
        company.setActive(true); // Default to active on creation

        Company savedCompany = companyRepository.save(company);
        return mapToCompanyResponseDTO(savedCompany);
    }

    @Transactional
    public CompanyResponseDTO updateCompany(Long companyId, CompanyUpdateRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        if (request.getName() != null && !request.getName().isBlank()) {
            // Check if new name conflicts with another existing company
            companyRepository.findByName(request.getName()).ifPresent(existingCompany -> {
                if (!existingCompany.getId().equals(companyId)) {
                    throw new DataIntegrityViolationException("Another company with name '" + request.getName() + "' already exists.");
                }
            });
            company.setName(request.getName());
        }
        if (request.getAddress() != null) { // Address can be blank or null to clear it
            company.setAddress(request.getAddress().isBlank() ? null : request.getAddress());
        }
        if (request.getIsActive() != null) {
            company.setActive(request.getIsActive());
        }

        Company updatedCompany = companyRepository.save(company);
        return mapToCompanyResponseDTO(updatedCompany);
    }

    @Transactional(readOnly = true)
    public CompanyResponseDTO getCompanyById(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));
        return mapToCompanyResponseDTO(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponseDTO> getAllCompanies(Boolean isActiveFilter) {
        List<Company> companies;
        if (isActiveFilter != null) {
            companies = companyRepository.findByIsActive(isActiveFilter);
        } else {
            companies = companyRepository.findAll();
        }
        return companies.stream()
                .map(this::mapToCompanyResponseDTO)
                .collect(Collectors.toList());
    }

    private CompanyResponseDTO mapToCompanyResponseDTO(Company company) {
        return new CompanyResponseDTO(
                company.getId(),
                company.getName(),
                company.getAddress(),
                company.isActive(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
