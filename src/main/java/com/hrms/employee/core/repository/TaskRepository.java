package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Custom query methods can be added here later if needed
    // List<Task> findByAssignedTo(User assignedTo);
    // List<Task> findByAssignedBy(User assignedBy);
    // List<Task> findByAssignedToAndStatus(User assignedTo, TaskStatus status);
}
