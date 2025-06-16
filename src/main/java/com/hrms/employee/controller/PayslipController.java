package com.hrms.employee.controller;

import com.hrms.employee.payload.response.PayslipDownloadInfoDTO;
import com.hrms.employee.payload.response.PayslipListResponse;
import com.hrms.employee.service.PayslipService;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.io.FileNotFoundException;
import java.util.Map;

@RestController
@RequestMapping("/api/employee/payslips")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_LEAD') or hasRole('ROLE_MANAGER') or hasRole('ROLE_HR')")
public class PayslipController {

    @Autowired
    private PayslipService payslipService;

    @GetMapping("/")
    public ResponseEntity<PayslipListResponse> getMyPayslips(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        PayslipListResponse response = payslipService.getMyPayslips(currentUser);
        return ResponseEntity.ok(response);
    }

    /*
    @GetMapping("/{payslipId}/download-info")
    public ResponseEntity<PayslipDownloadInfoDTO> downloadPayslipInfo(
            @PathVariable Long payslipId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        PayslipDownloadInfoDTO downloadInfo = payslipService.getPayslipDownloadInfo(payslipId, currentUser);
        return ResponseEntity.ok(downloadInfo);
    }
    */

    @GetMapping("/{payslipId}/download")
    public ResponseEntity<Resource> downloadActualPayslipFile(
            @PathVariable Long payslipId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            Map<String, Object> fileData = payslipService.loadPayslipFileAsResource(payslipId, currentUser);
            Resource resource = (Resource) fileData.get("resource");
            String filename = (String) fileData.get("filename");

            String contentType = MediaType.APPLICATION_PDF_VALUE; // Assuming PDF, can be made dynamic if needed

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (FileNotFoundException e) {
            // Log exception e
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            // Log exception e
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            // Log exception e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
