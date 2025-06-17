package com.hrms.hr.specs;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.User;
import com.hrms.employee.core.entity.InvestmentDeclaration;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class InvestmentDeclarationSpecification {

    public static Specification<InvestmentDeclaration> filterDeclarations(
            Long companyId,
            String status) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (companyId != null) {
                Join<InvestmentDeclaration, User> employeeJoin = root.join("employee");
                Join<User, Company> companyJoin = employeeJoin.join("company");
                predicates.add(criteriaBuilder.equal(companyJoin.get("id"), companyId));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status.toUpperCase()));
            }

            // Default ordering, can be overridden by Pageable in repository call
            query.orderBy(criteriaBuilder.desc(root.get("submittedAt")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
