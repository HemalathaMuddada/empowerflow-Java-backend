package com.hrms.superadmin.service;

import com.hrms.audit.repository.AuditLogRepository;
import com.hrms.config.repository.SystemConfigurationRepository;
import com.hrms.core.repository.CompanyRepository;
import com.hrms.core.repository.UserRepository;
import com.hrms.performancemanagement.repository.PerformanceReviewRepository;
import com.hrms.performancemanagement.repository.ReviewCycleRepository;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.superadmin.payload.response.SystemStatisticsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SuperAdminStatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(SuperAdminStatisticsService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private SystemConfigurationRepository systemConfigurationRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ReviewCycleRepository reviewCycleRepository;
    @Autowired private PerformanceReviewRepository performanceReviewRepository;

    @Transactional(readOnly = true)
    public SystemStatisticsDTO getSystemStatistics(UserDetailsImpl superAdminUser) {
        logger.info("SuperAdmin {} requesting system statistics.", superAdminUser.getUsername());

        SystemStatisticsDTO stats = new SystemStatisticsDTO();

        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countByIsActiveTrue());
        // stats.setInactiveUsers(userRepository.countByIsActiveFalse());
        // Or calculate: total - active
        stats.setInactiveUsers(stats.getTotalUsers() - stats.getActiveUsers());


        stats.setTotalCompanies(companyRepository.count());
        stats.setActiveCompanies(companyRepository.countByIsActiveTrue());
        // stats.setInactiveCompanies(companyRepository.countByIsActiveFalse());
        // Or calculate: total - active
        stats.setInactiveCompanies(stats.getTotalCompanies() - stats.getActiveCompanies());

        stats.setTotalSystemConfigurations(systemConfigurationRepository.count());
        stats.setTotalAuditLogEntries(auditLogRepository.count());
        stats.setPerformanceReviewCyclesCount(reviewCycleRepository.count());

        // Define what constitutes an "active" performance review for stats
        List<String> activeReviewStatuses = List.of(
            "PENDING_SELF_APPRAISAL",
            "PENDING_MANAGER_REVIEW",
            "PENDING_EMPLOYEE_ACKNOWLEDGEMENT",
            "PENDING_HR_REVIEW"
            // Not including "COMPLETED", "CLOSED", "ARCHIVED", "CANCELLED"
        );
        stats.setActivePerformanceReviewsCount(performanceReviewRepository.countByStatusIn(activeReviewStatuses));

        logger.debug("System statistics compiled: {}", stats);
        return stats;
    }
}
