package com.hrms.config.repository;

import com.hrms.config.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {
    // String is the type of the ID (@Id field configKey)
}
