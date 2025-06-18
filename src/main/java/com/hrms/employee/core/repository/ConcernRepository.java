package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.Concern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcernRepository extends JpaRepository<Concern, Long> {
    // Assuming company context is derived from the user who raised the concern.
    long countByStatusAndRaisedBy_CompanyId(com.hrms.employee.core.enums.ConcernStatus status, Long companyId);
    long countByStatus(com.hrms.employee.core.enums.ConcernStatus status);

    // List<Concern> findByRaisedBy(User raisedBy);
    // List<Concern> findByRaisedAgainstEmployeeOrRaisedAgainstLeadOrRaisedAgainstManager(User employee, User lead, User manager);
    // List<Concern> findByStatus(ConcernStatus status);
}
