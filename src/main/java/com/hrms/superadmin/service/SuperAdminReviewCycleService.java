package com.hrms.superadmin.service;

import com.hrms.audit.service.AuditLogService;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.performancemanagement.entity.ReviewCycle;
import com.hrms.performancemanagement.repository.PerformanceReviewRepository; // For delete check
import com.hrms.performancemanagement.repository.ReviewCycleRepository;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.superadmin.payload.request.ReviewCycleRequest;
import com.hrms.superadmin.payload.response.ReviewCycleResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; // For audit log
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Local exceptions for this service
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}

@Service
public class SuperAdminReviewCycleService {
    private static final Logger logger = LoggerFactory.getLogger(SuperAdminReviewCycleService.class);

    @Autowired
    private ReviewCycleRepository reviewCycleRepository;

    @Autowired
    private PerformanceReviewRepository performanceReviewRepository; // For checking associated reviews

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserRepository userRepository; // To fetch User entity for audit

    private ReviewCycleResponseDTO mapToDTO(ReviewCycle cycle) {
        return new ReviewCycleResponseDTO(
                cycle.getId(),
                cycle.getName(),
                cycle.getStartDate(),
                cycle.getEndDate(),
                cycle.getStatus(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt()
        );
    }

    @Transactional
    public ReviewCycleResponseDTO createReviewCycle(ReviewCycleRequest request, UserDetailsImpl superAdminUserDetails) {
        reviewCycleRepository.findByName(request.getName()).ifPresent(rc -> {
            throw new DataIntegrityViolationException("Review cycle with name '" + request.getName() + "' already exists.");
        });
        // DTO already validates dates chronological by @AssertTrue

        User superAdmin = userRepository.findById(superAdminUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SuperAdmin user not found."));

        ReviewCycle cycle = new ReviewCycle();
        cycle.setName(request.getName());
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());
        cycle.setStatus(request.getStatus().toUpperCase());
        // createdAt, updatedAt are handled by auditing

        ReviewCycle savedCycle = reviewCycleRepository.save(cycle);

        auditLogService.logEvent(
                superAdminUserDetails.getUsername(),
                superAdmin.getId(),
                "REVIEW_CYCLE_CREATE",
                "ReviewCycle",
                String.valueOf(savedCycle.getId()),
                String.format("Review cycle '%s' created. Period: %s to %s. Status: %s.",
                              savedCycle.getName(), savedCycle.getStartDate(), savedCycle.getEndDate(), savedCycle.getStatus()),
                null, "SUCCESS"
        );
        return mapToDTO(savedCycle);
    }

    @Transactional
    public ReviewCycleResponseDTO updateReviewCycle(Long cycleId, ReviewCycleRequest request, UserDetailsImpl superAdminUserDetails) {
        ReviewCycle cycle = reviewCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Review cycle not found with ID: " + cycleId));

        User superAdmin = userRepository.findById(superAdminUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SuperAdmin user not found."));

        // Check for name uniqueness if name is being changed
        if (!cycle.getName().equals(request.getName())) {
            reviewCycleRepository.findByName(request.getName()).ifPresent(rc -> {
                throw new DataIntegrityViolationException("Review cycle with name '" + request.getName() + "' already exists.");
            });
        }

        String oldDetails = String.format("Name: %s, Start: %s, End: %s, Status: %s",
                                          cycle.getName(), cycle.getStartDate(), cycle.getEndDate(), cycle.getStatus());

        cycle.setName(request.getName());
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());
        cycle.setStatus(request.getStatus().toUpperCase());

        ReviewCycle updatedCycle = reviewCycleRepository.save(cycle);

        String newDetails = String.format("Name: %s, Start: %s, End: %s, Status: %s",
                                          updatedCycle.getName(), updatedCycle.getStartDate(), updatedCycle.getEndDate(), updatedCycle.getStatus());
        auditLogService.logEvent(
                superAdminUserDetails.getUsername(),
                superAdmin.getId(),
                "REVIEW_CYCLE_UPDATE",
                "ReviewCycle",
                String.valueOf(updatedCycle.getId()),
                String.format("Review cycle ID %d updated. Old: [%s], New: [%s]", cycleId, oldDetails, newDetails),
                null, "SUCCESS"
        );
        return mapToDTO(updatedCycle);
    }

    @Transactional
    public void deleteReviewCycle(Long cycleId, UserDetailsImpl superAdminUserDetails) {
        ReviewCycle cycle = reviewCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Review cycle not found with ID: " + cycleId));

        // TODO: Business Rule - Prevent deletion if active performance reviews are linked to this cycle.
        // Example check:
        if (performanceReviewRepository.existsByReviewCycleId(cycleId)) {
             throw new BadRequestException("Cannot delete review cycle ID " + cycleId + " as it has associated performance reviews. Consider archiving it instead.");
        }
        // For now, simple delete.

        reviewCycleRepository.delete(cycle);

        auditLogService.logEvent(
                superAdminUserDetails.getUsername(),
                superAdminUserDetails.getId(),
                "REVIEW_CYCLE_DELETE",
                "ReviewCycle",
                String.valueOf(cycleId),
                String.format("Review cycle '%s' (ID: %d) deleted.", cycle.getName(), cycleId),
                null, "SUCCESS"
        );
    }

    @Transactional(readOnly = true)
    public ReviewCycleResponseDTO getReviewCycleById(Long cycleId) {
        ReviewCycle cycle = reviewCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Review cycle not found with ID: " + cycleId));
        return mapToDTO(cycle);
    }

    @Transactional(readOnly = true)
    public Page<ReviewCycleResponseDTO> getAllReviewCycles(Pageable pageable) {
        Page<ReviewCycle> cyclesPage = reviewCycleRepository.findAll(pageable);
        List<ReviewCycleResponseDTO> dtoList = cyclesPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, cyclesPage.getTotalElements());
    }
}
