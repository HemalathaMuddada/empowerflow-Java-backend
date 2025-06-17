package com.hrms.performancemanagement.specs;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.performancemanagement.entity.PerformanceReview;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PerformanceReviewSpecification {

    public static Specification<PerformanceReview> filterReviewsForHR(
            Long companyId, // Nullable for global HR to see all, or specific company
            String status) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (companyId != null) {
                Join<PerformanceReview, User> employeeJoin = root.join("employee");
                Join<User, Company> companyJoin = employeeJoin.join("company");
                predicates.add(criteriaBuilder.equal(companyJoin.get("id"), companyId));
            }
            // If companyId is null, it implies global HR wants to see reviews from all companies
            // or reviews for employees not assigned to any company.

            if (StringUtils.hasText(status)) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status.toUpperCase()));
            }

            // Default ordering, can be overridden by Pageable in repository call
            query.orderBy(criteriaBuilder.desc(root.get("createdAt"))); // Order by creation of review record

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
