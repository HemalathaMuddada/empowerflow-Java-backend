package com.hrms.audit.service;

import com.hrms.audit.entity.AuditLog;
import com.hrms.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.hrms.audit.payload.response.AuditLogDTO; // Added DTO
import com.hrms.audit.specs.AuditLogSpecification; // Added Specification
import org.springframework.data.domain.Page; // Added Page
import org.springframework.data.domain.PageImpl; // Added PageImpl
import org.springframework.data.domain.Pageable; // Added Pageable
import org.springframework.data.jpa.domain.Specification; // Added Specification
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate; // Added LocalDate
import java.util.List; // Added List
import java.util.stream.Collectors; // Added Collectors

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction for audit logging
    public void logEvent(String actorUsername, Long actorId, String actionType,
                         String targetEntityType, String targetEntityId,
                         String details, String ipAddress, String status) {

        logger.info("Audit event triggered: Action [{}], Actor [{} ({})], Target [{}:{}], IP [{}], Status [{}]",
                    actionType, actorUsername, actorId, targetEntityType, targetEntityId, ipAddress, status);

        try {
            AuditLog auditLogEntry = AuditLog.builder()
                .actorUsername(actorUsername)
                .actorId(actorId)
                .actionType(actionType)
                .targetEntityType(targetEntityType)
                .targetEntityId(targetEntityId)
                .details(details)
                .ipAddress(ipAddress)
                .status(status)
                // .timestamp will be set by @CreationTimestamp via JPA entity listener
                .build();

            AuditLog savedEntry = auditLogRepository.save(auditLogEntry);
            logger.debug("Audit event persisted with ID: {}", savedEntry.getId());

        } catch (Exception e) {
            // Log error but do not throw to avoid impacting the main operation
            logger.error("Failed to save audit log event: Action [{}], Actor [{}]. Error: {} - {}",
                         actionType, actorUsername, e.getClass().getSimpleName(), e.getMessage());
            // Optionally, log more details from 'e' if needed, but avoid exposing sensitive info if this log is externalized.
        }
    }

    private AuditLogDTO mapToDTO(AuditLog entity) {
        return new AuditLogDTO(
            entity.getId(),
            entity.getTimestamp(),
            entity.getActorUsername(),
            entity.getActorId(),
            entity.getActionType(),
            entity.getTargetEntityType(),
            entity.getTargetEntityId(),
            entity.getDetails(),
            entity.getIpAddress(),
            entity.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogs(String filterByActor, String filterByActionType,
                                          LocalDate filterStartDate, LocalDate filterEndDate,
                                          Pageable pageable) {

        Specification<AuditLog> spec = AuditLogSpecification.filterAuditLogs(
            filterByActor, filterByActionType, filterStartDate, filterEndDate);

        Page<AuditLog> auditLogPage = auditLogRepository.findAll(spec, pageable);

        List<AuditLogDTO> dtoList = auditLogPage.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, auditLogPage.getTotalElements());
    }
}
