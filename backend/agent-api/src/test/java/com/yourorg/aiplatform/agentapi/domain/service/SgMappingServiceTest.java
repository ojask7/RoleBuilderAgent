package com.yourorg.aiplatform.agentapi.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yourorg.aiplatform.agentapi.adapter.client.AdClient;
import com.yourorg.aiplatform.agentapi.adapter.client.CmdbClient;
import com.yourorg.aiplatform.agentapi.adapter.client.SailPointClient;
import com.yourorg.aiplatform.agentapi.agent.tools.AdGroupTool;
import com.yourorg.aiplatform.agentapi.agent.tools.CmdbTool;
import com.yourorg.aiplatform.agentapi.agent.tools.SailPointTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SgMappingServiceTest {

    private SgMappingService service;

    @BeforeEach
    void setup() {
        service = new SgMappingService(
            new SailPointTool(new SailPointClient()),
            new CmdbTool(new CmdbClient()),
            new AdGroupTool(new AdClient())
        );
    }

    @Test
    void deriveMappingInsightReturnsStubbedData() {
        var group = service.enrichSecurityGroup("SG-BILLING-ADMINS", "Billing admin access");
        var insight = service.deriveMappingInsight(group);

        assertThat(insight.applicationService()).isEqualTo("APP_CORE_BILLING");
        assertThat(insight.businessApplication()).isEqualTo("BA-DigitalIdentity");
        assertThat(insight.owner()).contains("yourorg.com");
    }
}
