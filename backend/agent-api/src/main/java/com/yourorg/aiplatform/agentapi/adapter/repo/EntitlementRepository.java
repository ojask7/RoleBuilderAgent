package com.yourorg.aiplatform.agentapi.adapter.repo;

import com.yourorg.aiplatform.agentapi.domain.model.Entitlement;
import com.yourorg.aiplatform.agentapi.domain.model.EntitlementStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

    Optional<Entitlement> findBySourceSgName(String sourceSgName);

    Page<Entitlement> findByStatus(EntitlementStatus status, Pageable pageable);

    List<Entitlement> findByStatus(EntitlementStatus status);

    long countByStatus(EntitlementStatus status);

    List<Entitlement> findByBusinessApp(String businessApp);
}
