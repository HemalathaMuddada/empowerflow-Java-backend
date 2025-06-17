package com.hrms.audit.specs;

import com.hrms.audit.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    public static Specification<AuditLog> filterAuditLogs(
            String actorUsername,
            String actionType,
            LocalDate startDate,
            LocalDate endDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(actorUsername)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("actorUsername")),
                                                     "%" + actorUsername.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(actionType)) {
                predicates.add(criteriaBuilder.equal(root.get("actionType"), actionType));
            }

            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("timestamp"),
                                                     startDate.atStartOfDay(),
                                                     endDate.atTime(LocalTime.MAX)));
            } else if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), startDate.atStartOfDay()));
            } else if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), endDate.atTime(LocalTime.MAX)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
