package com.yourorg.aiplatform.agentapi.adapter.repo;

import com.yourorg.aiplatform.agentapi.domain.model.BusinessRole;
import com.yourorg.aiplatform.agentapi.domain.model.BusinessRoleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRoleRepository extends JpaRepository<BusinessRole, Long> {

    Optional<BusinessRole> findByName(String name);

    List<BusinessRole> findByStatus(BusinessRoleStatus status);

    List<BusinessRole> findByDepartment(String department);

    List<BusinessRole> findByJobFunction(String jobFunction);
}
