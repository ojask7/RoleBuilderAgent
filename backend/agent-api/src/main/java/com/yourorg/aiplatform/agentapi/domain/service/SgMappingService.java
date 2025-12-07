package com.yourorg.aiplatform.agentapi.domain.service;

import com.yourorg.aiplatform.agentapi.agent.tools.AdGroupTool;
import com.yourorg.aiplatform.agentapi.agent.tools.CmdbTool;
import com.yourorg.aiplatform.agentapi.agent.tools.SailPointTool;
import com.yourorg.aiplatform.agentapi.domain.model.SecurityGroup;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SgMappingService {

    private final SailPointTool sailPointTool;
    private final CmdbTool cmdbTool;
    private final AdGroupTool adGroupTool;

    public SecurityGroup enrichSecurityGroup(String name, String description) {
        String owner = adGroupTool.sampleMembers(name).stream().findFirst().orElse("owner@yourorg.com");
        return SecurityGroup.builder()
            .name(name)
            .description(description)
            .owner(owner)
            .build();
    }

    public MappingInsight deriveMappingInsight(SecurityGroup group) {
        List<String> candidateServices = sailPointTool.fetchApplicationServices(group.getName());
        String topService = candidateServices.isEmpty() ? "UNKNOWN" : candidateServices.get(0);
        Map<String, String> cmdbRecord = cmdbTool.lookupBusinessApplication(topService);
        return MappingInsight.builder()
            .applicationService(topService)
            .businessApplication(cmdbRecord.getOrDefault("businessApplication", "N/A"))
            .owner(cmdbRecord.getOrDefault("owner", group.getOwner()))
            .build();
    }

    @Builder
    public record MappingInsight(String applicationService, String businessApplication, String owner) {
    }
}
