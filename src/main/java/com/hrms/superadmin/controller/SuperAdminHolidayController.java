package com.hrms.superadmin.controller;

import com.hrms.employee.payload.response.HolidayDetailsDTO; // Reusing
import com.hrms.hr.payload.request.HolidayManagementRequest;   // Reusing
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.superadmin.service.ResourceNotFoundException; // Assuming defined in SuperAdminHolidayService
import com.hrms.superadmin.service.SuperAdminHolidayService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/holidays/global")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminHolidayController {

    @Autowired
    private SuperAdminHolidayService superAdminHolidayService;

    @PostMapping("/")
    public ResponseEntity<HolidayDetailsDTO> createGlobalHoliday(
            @Valid @RequestBody HolidayManagementRequest request,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            HolidayDetailsDTO newHoliday = superAdminHolidayService.createGlobalHoliday(request, superAdminUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(newHoliday);
        } catch (Exception ex) { // Catch-all for unexpected issues
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating global holiday", ex);
        }
    }

    @PutMapping("/{holidayId}")
    public ResponseEntity<HolidayDetailsDTO> updateGlobalHoliday(
            @PathVariable Long holidayId,
            @Valid @RequestBody HolidayManagementRequest request,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            HolidayDetailsDTO updatedHoliday = superAdminHolidayService.updateGlobalHoliday(holidayId, request, superAdminUser);
            return ResponseEntity.ok(updatedHoliday);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating global holiday", ex);
        }
    }

    @DeleteMapping("/{holidayId}")
    public ResponseEntity<Void> deleteGlobalHoliday(
            @PathVariable Long holidayId,
            @AuthenticationPrincipal UserDetailsImpl superAdminUser) {
        try {
            superAdminHolidayService.deleteGlobalHoliday(holidayId, superAdminUser);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting global holiday", ex);
        }
    }

    @GetMapping("/")
    public ResponseEntity<List<HolidayDetailsDTO>> getAllGlobalHolidays() {
        try {
            List<HolidayDetailsDTO> holidays = superAdminHolidayService.getAllGlobalHolidays();
            return ResponseEntity.ok(holidays);
        } catch (Exception ex) {
            // Log ex
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching global holidays", ex);
        }
    }
}
