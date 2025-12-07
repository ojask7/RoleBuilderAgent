package com.yourorg.aiplatform.agentapi.adapter.repo;

import com.yourorg.aiplatform.agentapi.domain.model.SecurityGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityGroupRepository extends JpaRepository<SecurityGroup, Long> {

    Optional<SecurityGroup> findByName(String name);
}
