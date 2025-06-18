package com.hrms.hr.controller;

import com.hrms.employee.payload.response.HolidayDetailsDTO; // Reusing
import com.hrms.hr.payload.request.HolidayManagementRequest;
import com.hrms.hr.service.HRHolidayService;
import com.hrms.hr.service.ResourceNotFoundException; // Assuming this is accessible
import com.hrms.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/hr/holidays")
@PreAuthorize("hasAnyRole('ROLE_HR', 'ROLE_MANAGER')")
public class HRHolidayController {

    @Autowired
    private HRHolidayService hrHolidayService;

    @PostMapping("/")
    public ResponseEntity<HolidayDetailsDTO> addHoliday(
            @Valid @RequestBody HolidayManagementRequest request,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            HolidayDetailsDTO newHoliday = hrHolidayService.addHoliday(request, hrUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(newHoliday);
        } catch (IllegalStateException | ResourceNotFoundException ex) { // Catching specific business logic errors
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error adding holiday", ex);
        }
    }

    @PutMapping("/{holidayId}")
    public ResponseEntity<HolidayDetailsDTO> updateHoliday(
            @PathVariable Long holidayId,
            @Valid @RequestBody HolidayManagementRequest request,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            HolidayDetailsDTO updatedHoliday = hrHolidayService.updateHoliday(holidayId, request, hrUser);
            return ResponseEntity.ok(updatedHoliday);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating holiday", ex);
        }
    }

    @DeleteMapping("/{holidayId}")
    public ResponseEntity<Void> deleteHoliday(
            @PathVariable Long holidayId,
            @AuthenticationPrincipal UserDetailsImpl hrUser) {
        try {
            hrHolidayService.deleteHoliday(holidayId, hrUser);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting holiday", ex);
        }
    }
}
