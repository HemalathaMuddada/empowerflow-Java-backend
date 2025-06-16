package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Hike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HikeRepository extends JpaRepository<Hike, Long> {
    List<Hike> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);
    List<Hike> findByEmployeeIdInOrderByEmployee_IdAscEffectiveDateDesc(List<Long> employeeIds);
    // List<Hike> findByEmployee(User employee); // Alternative if passing the whole User object
}
