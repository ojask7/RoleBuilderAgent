package com.yourorg.aiplatform.agentapi.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.aiplatform.agentapi.agent.pipeline.Kc27VerificationAgent;
import com.yourorg.aiplatform.agentapi.agent.pipeline.SgMappingAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AgentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Kc27VerificationAgent kc27VerificationAgent;

    @MockBean
    private SgMappingAgent sgMappingAgent;

    @Test
    void kc27EndpointReturnsMockedResponse() throws Exception {
        when(kc27VerificationAgent.evaluate(any()))
            .thenReturn(new Kc27VerificationAgent.Kc27EvaluationResult("COMPLIANT", 0.9, "ok"));

        var payload = new AgentController.Kc27VerificationRequest(
            "SG-BILLING-ADMINS",
            "Billing admin group",
            java.util.List.of("APP_CORE_BILLING"),
            java.util.List.of("BA-Finance"),
            "owner@corp"
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/agents/kc27/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("COMPLIANT"));
    }

    @Test
    void sgMappingEndpointReturnsMockedResponse() throws Exception {
        when(sgMappingAgent.mapSecurityGroup(any()))
            .thenReturn(new SgMappingAgent.SgMappingResult(
                "SG-BILLING-ADMINS",
                "APP_CORE_BILLING",
                "BA-DigitalIdentity",
                "iam@yourorg.com",
                "stub"));

        var payload = new AgentController.SgMappingRequest("SG-BILLING-ADMINS", "Billing admin group");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/agents/sg/mapping")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.suggestedApplicationService").value("APP_CORE_BILLING"));
    }
}
