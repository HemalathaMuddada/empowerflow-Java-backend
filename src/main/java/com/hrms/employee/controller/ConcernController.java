package com.hrms.employee.controller;

import com.hrms.employee.payload.request.RaiseConcernRequest;
import com.hrms.employee.payload.response.ConcernResponseDTO;
import com.hrms.employee.service.ConcernService;
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/concerns")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class ConcernController {

    @Autowired
    private ConcernService concernService;

    @PostMapping("/raise")
    public ResponseEntity<ConcernResponseDTO> raiseNewConcern(
            @Valid @RequestBody RaiseConcernRequest concernRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        ConcernResponseDTO response = concernService.raiseConcern(concernRequest, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
