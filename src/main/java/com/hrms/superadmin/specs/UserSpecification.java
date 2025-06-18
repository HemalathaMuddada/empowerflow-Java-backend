package com.hrms.superadmin.specs;

import com.hrms.core.entity.Company;
import com.hrms.core.entity.Role;
import com.hrms.core.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType; // Added for JoinType.LEFT
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> filterUsers(Long companyId, String roleName, Boolean isActive, String designationFilter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (companyId != null) {
                // Assuming 'company' is a field in User entity which is a Company entity
                Join<User, Company> companyJoin = root.join("company"); // Default is INNER JOIN
                predicates.add(criteriaBuilder.equal(companyJoin.get("id"), companyId));
            }

            if (StringUtils.hasText(roleName)) {
                Join<User, Role> rolesJoin = root.join("roles", JoinType.LEFT); // Use LEFT JOIN if a user might not have roles
                predicates.add(criteriaBuilder.equal(rolesJoin.get("name"), roleName));
                query.distinct(true); // Add distinct due to join with roles
            }

            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }

            if (StringUtils.hasText(designationFilter)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("designation")), "%" + designationFilter.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
