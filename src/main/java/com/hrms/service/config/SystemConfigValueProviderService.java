package com.hrms.service.config;

import com.hrms.config.entity.SystemConfiguration;
import com.hrms.config.repository.SystemConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class SystemConfigValueProviderService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigValueProviderService.class);
    private static final String CACHE_NAME = "systemConfigurations";

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Cacheable(value = CACHE_NAME, key = "#key")
    public String getStringValue(String key, String defaultValue) {
        logger.debug("Fetching string value for key: {}", key);
        Optional<SystemConfiguration> configOpt = systemConfigurationRepository.findById(key);
        if (configOpt.isPresent()) {
            SystemConfiguration config = configOpt.get();
            // Basic type check, could be more stringent if needed
            if (!"STRING".equalsIgnoreCase(config.getValueType()) &&
                !"JSON".equalsIgnoreCase(config.getValueType()) && // Allow JSON to be read as String
                !"NUMBER".equalsIgnoreCase(config.getValueType()) && // Allow NUMBER to be read as String
                !"BOOLEAN".equalsIgnoreCase(config.getValueType()) && // Allow BOOLEAN to be read as String
                !"TIME".equalsIgnoreCase(config.getValueType())) { // Allow TIME to be read as String
                // This warning is for when a specific type getter (like getDoubleValue) calls this,
                // and the underlying type is unexpected even for a string representation.
                // For direct getStringValue calls, most types can be represented as strings.
                logger.warn("Configuration key '{}' has type '{}' but was fetched as STRING. Value: '{}'",
                            key, config.getValueType(), config.getConfigValue());
            }
            return config.getConfigValue();
        }
        logger.warn("Configuration key '{}' not found. Returning default value: {}", key, defaultValue);
        return defaultValue;
    }

    @Cacheable(value = CACHE_NAME, key = "#key")
    public Double getDoubleValue(String key, Double defaultValue) {
        logger.debug("Fetching double value for key: {}", key);
        Optional<SystemConfiguration> configOpt = systemConfigurationRepository.findById(key);
        if (configOpt.isPresent()) {
            SystemConfiguration config = configOpt.get();
            if (!"NUMBER".equalsIgnoreCase(config.getValueType())) {
                logger.error("Configuration key '{}' has type '{}', expected NUMBER. Returning default value: {}",
                             key, config.getValueType(), defaultValue);
                return defaultValue;
            }
            try {
                return Double.parseDouble(config.getConfigValue());
            } catch (NumberFormatException e) {
                logger.error("Error parsing double value for key '{}': {}. Value: '{}'. Returning default value: {}",
                             key, e.getMessage(), config.getConfigValue(), defaultValue);
                return defaultValue;
            }
        }
        logger.warn("Configuration key '{}' not found. Returning default value: {}", key, defaultValue);
        return defaultValue;
    }

    @Cacheable(value = CACHE_NAME, key = "#key")
    public Integer getIntegerValue(String key, Integer defaultValue) {
        logger.debug("Fetching integer value for key: {}", key);
        Optional<SystemConfiguration> configOpt = systemConfigurationRepository.findById(key);
        if (configOpt.isPresent()) {
            SystemConfiguration config = configOpt.get();
            if (!"NUMBER".equalsIgnoreCase(config.getValueType())) {
                logger.error("Configuration key '{}' has type '{}', expected NUMBER for an integer. Returning default value: {}",
                             key, config.getValueType(), defaultValue);
                return defaultValue;
            }
            try {
                // Allow parsing from double string like "8.0" to int 8
                return (int) Double.parseDouble(config.getConfigValue());
            } catch (NumberFormatException e) {
                logger.error("Error parsing integer value for key '{}': {}. Value: '{}'. Returning default value: {}",
                             key, e.getMessage(), config.getConfigValue(), defaultValue);
                return defaultValue;
            }
        }
        logger.warn("Configuration key '{}' not found. Returning default value: {}", key, defaultValue);
        return defaultValue;
    }

    @Cacheable(value = CACHE_NAME, key = "#key")
    public LocalTime getLocalTimeValue(String key, LocalTime defaultValue) {
        logger.debug("Fetching LocalTime value for key: {}", key);
        Optional<SystemConfiguration> configOpt = systemConfigurationRepository.findById(key);
        if (configOpt.isPresent()) {
            SystemConfiguration config = configOpt.get();
            if (!"TIME".equalsIgnoreCase(config.getValueType())) {
                logger.error("Configuration key '{}' has type '{}', expected TIME. Returning default value: {}",
                             key, config.getValueType(), defaultValue);
                return defaultValue;
            }
            try {
                return LocalTime.parse(config.getConfigValue()); // Assumes ISO format HH:mm or HH:mm:ss
            } catch (DateTimeParseException e) {
                logger.error("Error parsing LocalTime value for key '{}': {}. Value: '{}'. Returning default value: {}",
                             key, e.getMessage(), config.getConfigValue(), defaultValue);
                return defaultValue;
            }
        }
        logger.warn("Configuration key '{}' not found. Returning default value: {}", key, defaultValue);
        return defaultValue;
    }

    @Cacheable(value = CACHE_NAME, key = "#key")
    public Boolean getBooleanValue(String key, Boolean defaultValue) {
        logger.debug("Fetching boolean value for key: {}", key);
        Optional<SystemConfiguration> configOpt = systemConfigurationRepository.findById(key);
        if (configOpt.isPresent()) {
            SystemConfiguration config = configOpt.get();
            if (!"BOOLEAN".equalsIgnoreCase(config.getValueType())) {
                logger.error("Configuration key '{}' has type '{}', expected BOOLEAN. Returning default value: {}",
                             key, config.getValueType(), defaultValue);
                return defaultValue;
            }
            String value = config.getConfigValue();
            if ("true".equalsIgnoreCase(value)) {
                return true;
            } else if ("false".equalsIgnoreCase(value)) {
                return false;
            } else {
                logger.error("Invalid boolean value for key '{}': '{}'. Returning default value: {}",
                             key, value, defaultValue);
                return defaultValue;
            }
        }
        logger.warn("Configuration key '{}' not found. Returning default value: {}", key, defaultValue);
        return defaultValue;
    }
}
