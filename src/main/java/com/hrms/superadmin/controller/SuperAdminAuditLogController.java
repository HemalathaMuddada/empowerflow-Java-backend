package com.hrms.superadmin.controller;

import com.hrms.audit.payload.response.AuditLogDTO;
import com.hrms.audit.service.AuditLogService;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/super-admin/audit-logs")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminAuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/")
    public ResponseEntity<Page<AuditLogDTO>> viewAuditLogs(
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp,desc") String[] sort,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser // For authorization, not directly used in service query yet
    ) {
        try {
            List<Sort.Order> orders = new ArrayList<>();
            if (sort[0].contains(",")) { // Multiple sort criteria
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    if (_sort.length == 2) {
                        orders.add(new Sort.Order(Sort.Direction.fromString(_sort[1]), _sort[0]));
                    } else {
                         orders.add(new Sort.Order(Sort.Direction.DESC, "timestamp")); // Default fallback
                    }
                }
            } else { // Single sort criterion
                 if (sort.length == 2) {
                    orders.add(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
                 } else { // Fallback for malformed single sort
                    orders.add(new Sort.Order(Sort.Direction.DESC, "timestamp"));
                 }
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
            Page<AuditLogDTO> auditLogPage = auditLogService.getAuditLogs(
                    actorUsername, actionType, startDate, endDate, pageable);
            return ResponseEntity.ok(auditLogPage);
        } catch (IllegalArgumentException ex) { // Catch issues from Sort.Direction.fromString
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort parameter: " + ex.getMessage(), ex);
        }
         catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching audit logs", ex);
        }
    }
}
