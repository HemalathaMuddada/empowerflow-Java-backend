package com.hrms.employee.core.repository;

import com.hrms.core.entity.User;
import com.hrms.employee.core.entity.Task;
import com.hrms.employee.core.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedToIdAndStatusOrderByDeadlineAscPriorityDesc(Long assignedToId, TaskStatus status);
    List<Task> findByAssignedToIdOrderByDeadlineAscPriorityDesc(Long assignedToId);
    Optional<Task> findByIdAndAssignedToId(Long id, Long assignedToId);

    @Query("SELECT t FROM Task t WHERE t.deadline < :now AND t.status IN :openStatuses AND t.autoClosedAt IS NULL")
    List<Task> findOverdueTasksForAutoClosure(@Param("now") LocalDateTime now, @Param("openStatuses") List<TaskStatus> openStatuses);

    long countByAssignedToAndStatusInAndAutoClosedAtIsNull(User assignedTo, List<TaskStatus> statuses);
    long countByAssignedByAndStatusInAndDeadlineBeforeAndAutoClosedAtIsNull(User assignedBy, List<TaskStatus> statuses, LocalDateTime now);


  
}
