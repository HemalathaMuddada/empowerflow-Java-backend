package com.hrms.performancemanagement.repository;

import com.hrms.performancemanagement.entity.ReviewCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List; // Added import for List

@Repository
public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, Long>, JpaSpecificationExecutor<ReviewCycle> {
    Optional<ReviewCycle> findByName(String name);
    List<ReviewCycle> findByStatus(String status); // Example useful query
}
