package com.hrms.core.repository;

import com.hrms.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> { // Added JpaSpecificationExecutor
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    List<User> findByManagerId(Long managerId);
    List<User> findByCompanyId(Long companyId);
    List<User> findByCompanyIdAndIsActive(Long companyId, boolean isActive);
    List<User> findByCompanyIsNullAndIsActive(boolean isActive);
    List<User> findByCompanyIsNull();
    long countByRolesContains(Role role);
    List<User> findByIsActiveTrue();
    long countByIsActiveTrue(); // New count method
    long countByCompanyIdAndIsActiveTrue(Long companyId); // New count method

    // Find active users in a specific company having a specific role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.company.id = :companyId AND r.name = :roleName AND u.isActive = true")
    List<User> findByCompanyIdAndRoleNameAndIsActiveTrue(@Param("companyId") Long companyId, @Param("roleName") String roleName);

    long countByCompanyIdAndCreatedAtAfter(Long companyId, java.time.LocalDateTime date);
    long countByCreatedAtAfter(java.time.LocalDateTime date);

    List<User> findByManagerIdAndIsActiveTrue(Long managerId);
    boolean existsByManagerIdAndIsActiveTrue(Long managerId);
    long countByIsActiveFalse(); // Added
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "roles"})
    org.springframework.data.domain.Page<User> findAllWithCompanyAndRoles(org.springframework.data.jpa.domain.Specification<User> spec, org.springframework.data.domain.Pageable pageable);

}
