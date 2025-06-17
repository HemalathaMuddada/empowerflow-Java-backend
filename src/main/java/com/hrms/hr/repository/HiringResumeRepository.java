package com.hrms.hr.repository;

import com.hrms.hr.entity.HiringResume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // For complex filtering
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HiringResumeRepository extends JpaRepository<HiringResume, Long>, JpaSpecificationExecutor<HiringResume> {
    // Basic finders, more complex ones can use Specifications
    List<HiringResume> findByCategory(String category);
    List<HiringResume> findBySkillsContainingIgnoreCase(String skill);
    List<HiringResume> findByCategoryAndSkillsContainingIgnoreCase(String category, String skill);
}
