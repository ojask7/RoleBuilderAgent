package com.yourorg.aiplatform.agentapi.adapter.repo;

import com.yourorg.aiplatform.agentapi.domain.model.AccessBundle;
import com.yourorg.aiplatform.agentapi.domain.model.BundleStatus;
import com.yourorg.aiplatform.agentapi.domain.model.Kc27Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessBundleRepository extends JpaRepository<AccessBundle, Long> {

    Optional<AccessBundle> findByBusinessRoleId(Long businessRoleId);

    List<AccessBundle> findByStatus(BundleStatus status);

    List<AccessBundle> findByKc27Status(Kc27Status kc27Status);

    long countByStatus(BundleStatus status);

    long countByKc27Status(Kc27Status kc27Status);
}
