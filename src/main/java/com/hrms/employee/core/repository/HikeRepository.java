package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Hike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HikeRepository extends JpaRepository<Hike, Long> {
    // Custom query methods can be added here later if needed
    // List<Hike> findByEmployee(User employee);
}
