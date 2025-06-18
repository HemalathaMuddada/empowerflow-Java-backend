package com.hrms.core.repository;

import com.hrms.core.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
    List<Company> findByIsActive(boolean isActive); // Returns a list
    long countByIsActiveTrue(); // New
    long countByIsActiveFalse(); // New
}
