package com.hrms.superadmin.service;

import com.hrms.config.entity.SystemConfiguration;
import com.hrms.config.repository.SystemConfigurationRepository;
import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.security.service.UserDetailsImpl;
import com.hrms.audit.service.AuditLogService;
import com.hrms.superadmin.payload.request.SystemConfigurationCreateRequest;
import com.hrms.superadmin.payload.request.SystemConfigurationUpdateRequest;
import com.hrms.superadmin.payload.response.SystemConfigurationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import com.hrms.exception.BadRequestException; // Added common exception
import com.hrms.exception.ResourceNotFoundException; // Added common exception

@Service
public class SuperAdminConfigService {

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    private SystemConfigurationDTO mapToDTO(SystemConfiguration entity) {
        String updatedByName = null;
        Long updatedById = null;
        if (entity.getUpdatedBy() != null) {
            updatedByName = entity.getUpdatedBy().getUsername();
            updatedById = entity.getUpdatedBy().getId();
        }
        return new SystemConfigurationDTO(
                entity.getConfigKey(),
                entity.getConfigValue(),
                entity.getDescription(),
                entity.getValueType(),
                entity.getUpdatedAt(),
                updatedByName,
                updatedById
        );
    }

    @Transactional(readOnly = true)
    public List<SystemConfigurationDTO> getAllConfigurations() {
        return systemConfigurationRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SystemConfigurationDTO getConfigurationByKey(String configKey) {
        SystemConfiguration entity = systemConfigurationRepository.findById(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration with key '" + configKey + "' not found."));
        return mapToDTO(entity);
    }

    @Transactional
    @CacheEvict(value = "systemConfigurations", key = "#request.configKey")
    public SystemConfigurationDTO createConfiguration(SystemConfigurationCreateRequest request, UserDetailsImpl superAdminUserDetails) {
        if (systemConfigurationRepository.existsById(request.getConfigKey())) {
            throw new DataIntegrityViolationException("Configuration key '" + request.getConfigKey() + "' already exists.");
        }

        User superAdminUser = userRepository.findById(superAdminUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Super Admin user performing the action not found."));

        validateConfigValueByType(request.getConfigValue(), request.getValueType());

        SystemConfiguration entity = new SystemConfiguration();
        entity.setConfigKey(request.getConfigKey());
        entity.setConfigValue(request.getConfigValue());
        entity.setDescription(request.getDescription());
        entity.setValueType(request.getValueType().toUpperCase());
        entity.setUpdatedBy(superAdminUser);

        SystemConfiguration savedEntity = systemConfigurationRepository.save(entity);

        String createDetails = String.format("Key: %s, Value: %s, Type: %s, Description: %s",
                                           savedEntity.getConfigKey(),
                                           savedEntity.getConfigValue(),
                                           savedEntity.getValueType(),
                                           savedEntity.getDescription());
        auditLogService.logEvent(
            superAdminUserDetails.getUsername(),
            superAdminUserDetails.getId(),
            "CONFIG_CREATE",
            "SystemConfiguration",
            savedEntity.getConfigKey(),
            createDetails,
            null, // IP Address
            "SUCCESS"
        );
        return mapToDTO(savedEntity);
    }

    @Transactional
    @CacheEvict(value = "systemConfigurations", key = "#configKey")
    public SystemConfigurationDTO updateConfiguration(String configKey, SystemConfigurationUpdateRequest request, UserDetailsImpl superAdminUserDetails) {
        SystemConfiguration entity = systemConfigurationRepository.findById(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration with key '" + configKey + "' not found."));

        User superAdminUser = userRepository.findById(superAdminUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Super Admin user performing the action not found."));

        String oldValue = entity.getConfigValue(); // Capture old value BEFORE any changes

        validateConfigValueByType(request.getConfigValue(), entity.getValueType());

        entity.setConfigValue(request.getConfigValue());
        entity.setUpdatedBy(superAdminUser);
        // updatedAt is set by @LastModifiedDate

        SystemConfiguration updatedEntity = systemConfigurationRepository.save(entity);

        String updateDetails = String.format("Key: %s, Old Value: %s, New Value: %s",
                                           updatedEntity.getConfigKey(),
                                           oldValue,
                                           updatedEntity.getConfigValue());
        auditLogService.logEvent(
            superAdminUserDetails.getUsername(),
            superAdminUserDetails.getId(),
            "CONFIG_UPDATE",
            "SystemConfiguration",
            updatedEntity.getConfigKey(),
            updateDetails,
            null, // IP Address
            "SUCCESS"
        );
        return mapToDTO(updatedEntity);
    }

    private void validateConfigValueByType(String value, String type) {
        String upperType = type.toUpperCase();
        try {
            switch (upperType) {
                case "NUMBER":
                    Double.parseDouble(value);
                    break;
                case "BOOLEAN":
                    if (!("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
                        throw new BadRequestException("Invalid value for BOOLEAN type. Must be 'true' or 'false'.");
                    }
                    break;
                case "TIME":
                    LocalTime.parse(value);
                    break;
                case "STRING":
                case "JSON":
                    break;
                default:
                    throw new BadRequestException("Unknown value type for validation: " + type);
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid value for NUMBER type: " + value + ". Details: " + e.getMessage());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid value for TIME type: " + value + ". Expected ISO format (e.g., HH:mm or HH:mm:ss). Details: " + e.getMessage());
        }
    }
}
