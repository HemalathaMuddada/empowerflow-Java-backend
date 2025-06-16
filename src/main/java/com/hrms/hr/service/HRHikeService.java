package com.hrms.hr.service;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Hike;
import com.hrms.employee.core.repository.HikeRepository;
import com.hrms.employee.payload.response.HikeDetailDTO; // Reusing
import com.hrms.hr.payload.request.HRManualHikeRecordRequest;
import com.hrms.hr.payload.request.PublishHikesRequest; // New DTO for publish
import com.hrms.hr.payload.response.HikeCsvUploadSummaryDTO;
import com.hrms.hr.payload.response.HikePublishSummaryDTO; // New DTO for publish summary
import com.hrms.security.service.UserDetailsImpl;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // For StringUtils.hasText
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger; // Added for logging
import org.slf4j.LoggerFactory; // Added for logging


// Assuming ResourceNotFoundException & BadRequestException are defined (e.g., in HREmployeeService or a common place)
// If not, they should be defined here or imported.

@Service
public class HRHikeService {

    private static final Logger logger = LoggerFactory.getLogger(HRHikeService.class);


    @Autowired
    private HikeRepository hikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;


    @Transactional
    public HikeDetailDTO addHikeRecordManually(HRManualHikeRecordRequest request, UserDetailsImpl hrUserDetails) {
        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Target employee not found with ID: " + request.getEmployeeId()));

        Company hrCompany = hrUser.getCompany();
        if (hrCompany == null && (employee.getCompany() != null)) {
             throw new AccessDeniedException("HR user without a company cannot manage hikes for company employees.");
        }
        if (hrCompany != null && (employee.getCompany() == null || !Objects.equals(employee.getCompany().getId(), hrCompany.getId()))) {
            throw new AccessDeniedException("HR can only manage hikes for employees within their own company.");
        }

        BigDecimal oldSalary = request.getOldSalary();
        BigDecimal newSalary;
        BigDecimal actualHikeAmount = request.getHikeAmount();
        BigDecimal actualHikePercentage = request.getHikePercentage();

        if (actualHikeAmount != null && actualHikeAmount.compareTo(BigDecimal.ZERO) >= 0) {
            newSalary = oldSalary.add(actualHikeAmount);
            if (oldSalary.compareTo(BigDecimal.ZERO) > 0) {
                actualHikePercentage = actualHikeAmount.multiply(new BigDecimal("100"))
                                                     .divide(oldSalary, 2, RoundingMode.HALF_UP);
            } else if (actualHikeAmount.compareTo(BigDecimal.ZERO) > 0) {
                actualHikePercentage = null;
            } else {
                 actualHikePercentage = BigDecimal.ZERO;
            }
        } else if (actualHikePercentage != null && actualHikePercentage.compareTo(BigDecimal.ZERO) >= 0) {
            BigDecimal percentageAsDecimal = actualHikePercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            actualHikeAmount = oldSalary.multiply(percentageAsDecimal).setScale(2, RoundingMode.HALF_UP);
            newSalary = oldSalary.add(actualHikeAmount);
        } else {
            throw new BadRequestException("Either hikePercentage or hikeAmount must be provided and non-negative.");
        }

        Hike hike = new Hike();
        hike.setEmployee(employee);
        hike.setOldSalary(oldSalary);
        hike.setHikePercentage(actualHikePercentage);
        hike.setHikeAmount(actualHikeAmount);
        hike.setNewSalary(newSalary);
        hike.setEffectiveDate(request.getEffectiveDate());
        hike.setPromotionTitle(request.getPromotionTitle());
        hike.setComments(request.getComments());
        hike.setProcessedBy(hrUser);
        hike.setProcessedAt(LocalDateTime.now());

        Hike savedHike = hikeRepository.save(hike);
        return mapToHikeDetailDTO(savedHike);
    }

    private HikeDetailDTO mapToHikeDetailDTO(Hike hike) {
        return new HikeDetailDTO(
                hike.getId(),
                hike.getHikePercentage(),
                hike.getHikeAmount(),
                hike.getOldSalary(),
                hike.getNewSalary(),
                hike.getEffectiveDate(),
                hike.getPromotionTitle(),
                hike.getComments(),
                hike.getProcessedAt()
        );
    }

    @Transactional
    public HikeCsvUploadSummaryDTO processHikeCsvUpload(MultipartFile file, UserDetailsImpl hrUserDetails) throws IOException {
        HikeCsvUploadSummaryDTO summary = new HikeCsvUploadSummaryDTO();
        if (file.isEmpty()) {
            summary.addErrorDetail("Uploaded file is empty.");
            summary.incrementFailedRecords();
            return summary;
        }

        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));
        Company hrCompany = hrUser.getCompany();

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                    .setHeader("EmployeeUsername", "OldSalary", "HikePercentage", "HikeAmount",
                               "EffectiveDate", "PromotionTitle", "Comments")
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build());

            for (CSVRecord csvRecord : csvParser) {
                summary.incrementTotalRecordsProcessed();
                try {
                    String employeeUsername = csvRecord.get("EmployeeUsername");
                    if (!StringUtils.hasText(employeeUsername)) {
                        throw new BadRequestException("EmployeeUsername is missing.");
                    }

                    User employee = userRepository.findByUsername(employeeUsername)
                            .orElseThrow(() -> new ResourceNotFoundException("Employee '" + employeeUsername + "' not found."));

                    if (hrCompany == null && employee.getCompany() != null) {
                        throw new AccessDeniedException("HR user without a company cannot manage hikes for company employees.");
                    }
                    if (hrCompany != null && (employee.getCompany() == null || !Objects.equals(employee.getCompany().getId(), hrCompany.getId()))) {
                        throw new AccessDeniedException("HR can only manage hikes for employees within their own company. Employee: " + employeeUsername);
                    }

                    BigDecimal oldSalary = new BigDecimal(csvRecord.get("OldSalary"));
                    String hikePercentageStr = csvRecord.get("HikePercentage");
                    String hikeAmountStr = csvRecord.get("HikeAmount");
                    LocalDate effectiveDate = LocalDate.parse(csvRecord.get("EffectiveDate"));
                    String promotionTitle = csvRecord.get("PromotionTitle");
                    String comments = csvRecord.get("Comments");

                    BigDecimal hikePercentage = StringUtils.hasText(hikePercentageStr) ? new BigDecimal(hikePercentageStr) : null;
                    BigDecimal hikeAmount = StringUtils.hasText(hikeAmountStr) ? new BigDecimal(hikeAmountStr) : null;

                    if ((hikePercentage == null || hikePercentage.compareTo(BigDecimal.ZERO) < 0) &&
                        (hikeAmount == null || hikeAmount.compareTo(BigDecimal.ZERO) < 0)) {
                        throw new BadRequestException("Either HikePercentage or HikeAmount must be provided and non-negative.");
                    }

                    BigDecimal newSalary;
                    BigDecimal actualHikeAmount = hikeAmount;
                    BigDecimal actualHikePercentage = hikePercentage;

                    if (actualHikeAmount != null && actualHikeAmount.compareTo(BigDecimal.ZERO) >= 0) {
                        newSalary = oldSalary.add(actualHikeAmount);
                         if (oldSalary.compareTo(BigDecimal.ZERO) > 0) {
                            actualHikePercentage = actualHikeAmount.multiply(new BigDecimal("100"))
                                                                 .divide(oldSalary, 2, RoundingMode.HALF_UP);
                        } else if (actualHikeAmount.compareTo(BigDecimal.ZERO) > 0) {
                            actualHikePercentage = null;
                        } else {
                            actualHikePercentage = BigDecimal.ZERO;
                        }
                    } else if (actualHikePercentage != null && actualHikePercentage.compareTo(BigDecimal.ZERO) >= 0) {
                        BigDecimal percentageAsDecimal = actualHikePercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                        actualHikeAmount = oldSalary.multiply(percentageAsDecimal).setScale(2, RoundingMode.HALF_UP);
                        newSalary = oldSalary.add(actualHikeAmount);
                    } else {
                         throw new BadRequestException("Logic error in hike calculation based on provided values.");
                    }

                    Hike hike = new Hike();
                    hike.setEmployee(employee);
                    hike.setOldSalary(oldSalary);
                    hike.setHikePercentage(actualHikePercentage);
                    hike.setHikeAmount(actualHikeAmount);
                    hike.setNewSalary(newSalary);
                    hike.setEffectiveDate(effectiveDate);
                    hike.setPromotionTitle(StringUtils.hasText(promotionTitle) ? promotionTitle : null);
                    hike.setComments(StringUtils.hasText(comments) ? comments : null);
                    hike.setProcessedBy(hrUser);
                    hike.setProcessedAt(LocalDateTime.now());

                    hikeRepository.save(hike);
                    summary.incrementSuccessfullyImported();

                } catch (ResourceNotFoundException | AccessDeniedException | BadRequestException | DateTimeParseException | NumberFormatException ex) {
                    summary.incrementFailedRecords();
                    summary.addErrorDetail("Row " + csvRecord.getRecordNumber() + " (User: " + csvRecord.get("EmployeeUsername") + "): " + ex.getMessage());
                } catch (Exception ex) {
                    summary.incrementFailedRecords();
                    summary.addErrorDetail("Row " + csvRecord.getRecordNumber() + " (User: " + csvRecord.get("EmployeeUsername") + "): Unexpected error - " + ex.getMessage());
                }
            }
        }
        return summary;
    }

    @Transactional
    public HikePublishSummaryDTO publishHikeRecords(PublishHikesRequest request, UserDetailsImpl hrUserDetails) {
        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));
        Company hrCompany = hrUser.getCompany();

        HikePublishSummaryDTO summary = new HikePublishSummaryDTO(request.getHikeRecordIds().size());

        for (Long hikeId : request.getHikeRecordIds()) {
            try {
                Hike hikeRecord = hikeRepository.findById(hikeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Hike record not found with ID: " + hikeId));

                User employee = hikeRecord.getEmployee();
                if (hrCompany == null && employee.getCompany() != null) {
                    summary.addDetail("Hike ID " + hikeId + ": Skipped. HR user without a company cannot manage this hike.");
                    summary.incrementPermissionDeniedSkipped();
                    continue;
                }
                if (hrCompany != null && (employee.getCompany() == null || !Objects.equals(employee.getCompany().getId(), hrCompany.getId()))) {
                    summary.addDetail("Hike ID " + hikeId + ": Skipped. Employee not in HR user's company.");
                    summary.incrementPermissionDeniedSkipped();
                    continue;
                }

                if (hikeRecord.getPublishedAt() != null) {
                    summary.addDetail("Hike ID " + hikeId + ": Skipped. Already published on " + hikeRecord.getPublishedAt());
                    summary.incrementAlreadyPublishedSkipped();
                    continue;
                }

                hikeRecord.setPublishedAt(LocalDateTime.now());
                hikeRepository.save(hikeRecord);
                summary.incrementSuccessfullyPublished();
                summary.addDetail("Hike ID " + hikeId + ": Published successfully.");

                if (mailSender != null && StringUtils.hasText(employee.getEmail())) {
                    try {
                        SimpleMailMessage message = new SimpleMailMessage();
                        message.setTo(employee.getEmail());

                        String employeeName = employee.getFirstName();
                        String effectiveDateStr = hikeRecord.getEffectiveDate().toString();
                        String hikePercentageStr = (hikeRecord.getHikePercentage() != null) ? hikeRecord.getHikePercentage().stripTrailingZeros().toPlainString() + "%" : "N/A";
                        String hikeAmountStr = (hikeRecord.getHikeAmount() != null) ? hikeRecord.getHikeAmount().stripTrailingZeros().toPlainString() : "N/A";
                        String newSalaryStr = (hikeRecord.getNewSalary() != null) ? hikeRecord.getNewSalary().stripTrailingZeros().toPlainString() : "N/A";
                        // Assuming currency symbol is not part of these values for now.
                        // Add it in formatting if needed, e.g., "$" + newSalaryStr

                        message.setSubject(String.format("Congratulations on Your Salary Revision, %s!", employeeName));

                        StringBuilder textBodyBuilder = new StringBuilder();
                        textBodyBuilder.append(String.format("Dear %s,\n\nWe are pleased to inform you of your salary revision, effective %s!\n\n", employeeName, effectiveDateStr));
                        textBodyBuilder.append("Your new compensation details are:\n");
                        if (hikeRecord.getHikePercentage() != null) textBodyBuilder.append(String.format("  - Hike Percentage: %s\n", hikePercentageStr));
                        if (hikeRecord.getHikeAmount() != null) textBodyBuilder.append(String.format("  - Hike Amount: %s\n", hikeAmountStr));
                        textBodyBuilder.append(String.format("  - New Salary: %s\n", newSalaryStr));

                        if (StringUtils.hasText(hikeRecord.getPromotionTitle())) {
                            textBodyBuilder.append(String.format("\nCongratulations on your promotion to %s as well!\n", hikeRecord.getPromotionTitle()));
                        }

                        if (StringUtils.hasText(hikeRecord.getHikeLetterDocumentUrl())) {
                            textBodyBuilder.append(String.format("\nYour promotion/hike letter is available here: %s\n", hikeRecord.getHikeLetterDocumentUrl()));
                            textBodyBuilder.append("Alternatively, you can find it in the Document Center in your employee portal if uploaded there separately.\n");
                        }

                        textBodyBuilder.append("\nRegards,\nHR Department");

                        message.setText(textBodyBuilder.toString());
                        // message.setFrom("noreply@hrms.example.com"); // Set from address from properties if needed
                        mailSender.send(message);
                        summary.incrementEmailSent();
                        summary.addDetail("Hike ID " + hikeId + ": Email notification sent to " + employee.getEmail());
                    } catch (MailException e) {
                        summary.incrementEmailFailed();
                        summary.addDetail("Hike ID " + hikeId + ": Failed to send email notification - " + e.getMessage());
                        logger.error("Failed to send hike notification email for Hike ID {}: {}", hikeId, e.getMessage(), e);
                    }
                } else {
                    summary.addDetail("Hike ID " + hikeId + ": Email notification skipped (MailSender not configured or employee email missing).");
                    if (mailSender == null) logger.warn("MailSender not configured. Cannot send hike email for Hike ID {}.", hikeId);
                    else logger.warn("Employee email missing for Hike ID {}. Cannot send email.", hikeId);
                }

            } catch (ResourceNotFoundException e) {
                summary.incrementNotFoundSkipped();
                summary.addDetail("Hike ID " + hikeId + ": Skipped. " + e.getMessage());
            } catch (AccessDeniedException e) {
                summary.incrementPermissionDeniedSkipped();
                summary.addDetail("Hike ID " + hikeId + ": Skipped. " + e.getMessage());
            } catch (Exception e) {
                summary.incrementFailedRecords();
                summary.addDetail("Hike ID " + hikeId + ": Failed to process - " + e.getMessage());
                logger.error("Unexpected error processing Hike ID {}: {}", hikeId, e.getMessage(), e);
            }
        }
        return summary;
    }
}
