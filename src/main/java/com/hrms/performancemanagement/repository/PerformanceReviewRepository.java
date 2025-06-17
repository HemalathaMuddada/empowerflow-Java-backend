package com.hrms.performancemanagement.repository;

import com.hrms.performancemanagement.entity.PerformanceReview;
import com.hrms.core.entity.User;
import com.hrms.performancemanagement.entity.ReviewCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long>, JpaSpecificationExecutor<PerformanceReview> {
    Optional<PerformanceReview> findByEmployeeAndReviewCycle(User employee, ReviewCycle reviewCycle);
    List<PerformanceReview> findByEmployeeOrderByReviewCycle_StartDateDesc(User employee);
    List<PerformanceReview> findByReviewerAndStatusOrderByReviewCycle_StartDateDesc(User reviewer, String status);
    List<PerformanceReview> findByReviewCycleAndStatus(ReviewCycle reviewCycle, String status); // Example additional query
    List<PerformanceReview> findByEmployee_IdAndReviewCycle_Id(Long employeeId, Long reviewCycleId); // For unique check or specific fetch
    boolean existsByReviewCycleId(Long reviewCycleId); // For checking before deleting a cycle
    Optional<PerformanceReview> findTopByEmployeeAndStatusNotInOrderByCreatedAtDesc(User employee, List<String> excludedStatuses);

    long countByStatusAndEmployee_CompanyId(String status, Long companyId);
    long countByStatus(String status);
    long countByReviewerAndStatusIn(User reviewer, List<String> statuses);
    long countByStatusIn(List<String> statuses); // Added for System Stats

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"employee", "employee.company", "reviewer", "reviewCycle", "reviewedBy"})
    org.springframework.data.domain.Page<PerformanceReview> findAllWithDetails(org.springframework.data.jpa.domain.Specification<PerformanceReview> spec, org.springframework.data.domain.Pageable pageable);

}
