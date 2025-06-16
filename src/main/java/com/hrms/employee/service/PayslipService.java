package com.hrms.employee.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Payslip;
import com.hrms.employee.core.repository.PayslipRepository;
import com.hrms.employee.payload.response.PayslipDownloadInfoDTO;
import com.hrms.employee.payload.response.PayslipListItemDTO;
import com.hrms.employee.payload.response.PayslipListResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PayslipService {

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${hrms.payslip.storage.base-path}")
    private String storageBasePath;

    @Transactional(readOnly = true)
    public PayslipListResponse getMyPayslips(UserDetailsImpl currentUserDetails) {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        List<Payslip> payslips = payslipRepository.findByEmployeeOrderByPayPeriodEndDesc(user);

        List<PayslipListItemDTO> dtoList = payslips.stream()
                .map(this::mapToPayslipListItemDTO)
                .collect(Collectors.toList());

        return new PayslipListResponse(dtoList);
    }

    @Transactional(readOnly = true)
    public PayslipDownloadInfoDTO getPayslipDownloadInfo(Long payslipId, UserDetailsImpl currentUserDetails) {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new RuntimeException("Payslip not found with id: " + payslipId)); // Or a custom PayslipNotFoundException

        // Security check: Ensure the payslip belongs to the current user
        if (!payslip.getEmployee().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to access this payslip.");
        }

        String fileName = String.format("payslip_%d_%02d_%d_emp%d.pdf",
                payslip.getPayPeriodEnd().getYear(),
                payslip.getPayPeriodEnd().getMonthValue(),
                payslip.getPayPeriodEnd().getDayOfMonth(), // Or just year_month
                user.getId());

        String fileUrlMock = payslip.getFileUrl() != null && !payslip.getFileUrl().isBlank()
                ? payslip.getFileUrl()
                               : String.format("/mock-download/payslips/%d/%s", payslip.getId(), fileName);


        return new PayslipDownloadInfoDTO(
                payslip.getId(),
                fileName,
                fileUrlMock,
                "Download initiated for " + fileName + ". Actual file download to be implemented."
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> loadPayslipFileAsResource(Long payslipId, UserDetailsImpl currentUserDetails) throws FileNotFoundException {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new FileNotFoundException("Payslip not found with id: " + payslipId));

        if (!payslip.getEmployee().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to access this payslip.");
        }

        if (!StringUtils.hasText(payslip.getFileUrl())) {
            throw new FileNotFoundException("Payslip file URL is missing for payslip id: " + payslipId);
        }

        try {
            Path filePath = Paths.get(storageBasePath, payslip.getFileUrl()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new FileNotFoundException("Payslip file not found or is not readable: " + payslip.getFileUrl());
            }
            Map<String, Object> result = new HashMap<>();
            result.put("resource", resource);
            // Use the stored fileUrl as the filename for download, assuming it's the actual file name.
            // If fileUrl is a full path or complex, extract filename part. For now, assume it's simple.
            result.put("filename", Paths.get(payslip.getFileUrl()).getFileName().toString());
            return result;

        } catch (MalformedURLException ex) {
            throw new FileNotFoundException("Payslip file path is invalid: " + payslip.getFileUrl());
        }
    }

    private PayslipListItemDTO mapToPayslipListItemDTO(Payslip payslip) {
        return new PayslipListItemDTO(
                payslip.getId(),
                payslip.getPayPeriodStart(),
                payslip.getPayPeriodEnd(),
                payslip.getNetSalary(),
                payslip.getGeneratedDate(),
                "Available" // Placeholder status
        );
    }
}
