package com.yourorg.aiplatform.agentapi.adapter.mapper;

import com.yourorg.aiplatform.agentapi.domain.model.SecurityGroup;
import org.springframework.stereotype.Component;

@Component
public class SecurityGroupMapper {

    public SecurityGroup toEntity(String name, String description, String owner) {
        return SecurityGroup.builder()
            .name(name)
            .description(description)
            .owner(owner)
            .build();
    }
}
