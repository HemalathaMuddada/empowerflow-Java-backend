package com.hrms.employee.core.repository;

import com.hrms.employee.core.entity.InvestmentDeclaration;
import com.hrms.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvestmentDeclarationRepository extends JpaRepository<InvestmentDeclaration, Long>, JpaSpecificationExecutor<InvestmentDeclaration> {
    List<InvestmentDeclaration> findByEmployeeOrderByDeclarationYearDescSubmittedAtDesc(User employee);

    // Example for HR: find by company and status (requires joining through User to Company)
    // @Query("SELECT id FROM InvestmentDeclaration id JOIN id.employee e WHERE e.company = :company AND id.status = :status")
    // List<InvestmentDeclaration> findByEmployee_CompanyAndStatus(Company company, String status);

    List<InvestmentDeclaration> findByEmployee_Company_IdAndStatus(Long companyId, String status);
    List<InvestmentDeclaration> findByEmployee_IdAndDeclarationYear(Long employeeId, String declarationYear);

}
