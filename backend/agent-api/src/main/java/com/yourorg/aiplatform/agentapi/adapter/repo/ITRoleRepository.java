package com.yourorg.aiplatform.agentapi.adapter.repo;

import com.yourorg.aiplatform.agentapi.domain.model.ITRole;
import com.yourorg.aiplatform.agentapi.domain.model.ITRoleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITRoleRepository extends JpaRepository<ITRole, Long> {

    Optional<ITRole> findByName(String name);

    List<ITRole> findByStatus(ITRoleStatus status);

    List<ITRole> findByApplicationId(String applicationId);
}
