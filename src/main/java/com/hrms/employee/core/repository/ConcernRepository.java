package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Concern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcernRepository extends JpaRepository<Concern, Long> {
    // Custom query methods can be added here later if needed
    // List<Concern> findByRaisedBy(User raisedBy);
    // List<Concern> findByRaisedAgainstEmployeeOrRaisedAgainstLeadOrRaisedAgainstManager(User employee, User lead, User manager);
    // List<Concern> findByStatus(ConcernStatus status);
}
