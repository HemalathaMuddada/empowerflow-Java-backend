package com.hrms.superadmin.controller;

import com.hrms.employee.payload.response.DocumentListItemDTO; // Reusing
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.superadmin.service.SuperAdminDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/super-admin/documents")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminDocumentController {

    @Autowired
    private SuperAdminDocumentService superAdminDocumentService;

    @GetMapping("/global-policies")
    public ResponseEntity<Page<DocumentListItemDTO>> viewAllGlobalPolicyDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser
    ) {
        try {
            List<Sort.Order> orders = new ArrayList<>();
            if (sort[0].contains(",")) { // Multiple sort criteria
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    if (_sort.length == 2) {
                        orders.add(new Sort.Order(Sort.Direction.fromString(_sort[1]), _sort[0]));
                    } else {
                        orders.add(new Sort.Order(Sort.Direction.DESC, "createdAt")); // Default fallback
                    }
                }
            } else { // Single sort criterion
                 if (sort.length == 2) {
                    orders.add(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
                 } else { // Fallback for malformed single sort
                    orders.add(new Sort.Order(Sort.Direction.DESC, "createdAt"));
                 }
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
            Page<DocumentListItemDTO> documentsPage = superAdminDocumentService.getAllGlobalPolicyDocuments(pageable, superAdminUser);
            return ResponseEntity.ok(documentsPage);
        } catch (IllegalArgumentException ex) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort parameter: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching global policy documents.", ex);
        }
    }
}
