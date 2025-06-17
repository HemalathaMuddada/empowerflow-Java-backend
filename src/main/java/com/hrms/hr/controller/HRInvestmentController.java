package com.hrms.hr.controller;

import com.hrms.employee.payload.response.InvestmentDeclarationDTO;
import com.hrms.hr.payload.request.HRDeclarationActionRequest;
import com.hrms.hr.service.HRInvestmentService;
import com.hrms.hr.service.BadRequestException; // Assuming accessible
import com.hrms.hr.service.ResourceNotFoundException; // Assuming accessible
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/hr/investments/declarations")
@PreAuthorize("hasAnyRole('ROLE_HR', 'ROLE_MANAGER')") // Managers might view, HR actions
public class HRInvestmentController {

    @Autowired
    private HRInvestmentService hrInvestmentService;

    @GetMapping("/")
    public ResponseEntity<Page<InvestmentDeclarationDTO>> getDeclarationsForCompany(
            @RequestParam(required = false) Long companyId, // Optional: Super/Global HR might specify
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "submittedAt,desc") String[] sort,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            List<Sort.Order> orders = new ArrayList<>();
            if (sort[0].contains(",")) {
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    if (_sort.length == 2) orders.add(new Sort.Order(Sort.Direction.fromString(_sort[1]), _sort[0]));
                    else orders.add(new Sort.Order(Sort.Direction.DESC, "submittedAt"));
                }
            } else {
                 if (sort.length == 2) orders.add(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
                 else orders.add(new Sort.Order(Sort.Direction.DESC, "submittedAt"));
            }
            Pageable pageable = PageRequest.of(page, size, Sort.by(orders));

            Page<InvestmentDeclarationDTO> declarations = hrInvestmentService.getDeclarationsForCompany(companyId, status, hrUser, pageable);
            return ResponseEntity.ok(declarations);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching investment declarations.", ex);
        }
    }

    @PostMapping("/{declarationId}/action")
    @PreAuthorize("hasRole('ROLE_HR')") // Only HR can action
    public ResponseEntity<InvestmentDeclarationDTO> takeDeclarationAction(
            @PathVariable Long declarationId,
            @Valid @RequestBody HRDeclarationActionRequest actionRequest,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            InvestmentDeclarationDTO updatedDeclaration = hrInvestmentService.approveOrRejectDeclaration(declarationId, actionRequest, hrUser);
            return ResponseEntity.ok(updatedDeclaration);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (BadRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error actioning investment declaration.", ex);
        }
    }
}
