package com.yourorg.aiplatform.agentapi.integration.servicenow;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ServiceNow connector for access request management.
 * Creates catalog items for Business Roles, handles request workflows,
 * and pushes Access Bundle approvals as RITM (Request Item) closures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceNowClient {

    @Value("${accessforge.servicenow.instance:}")
    private String instance;

    @Value("${accessforge.servicenow.username:}")
    private String username;

    @Value("${accessforge.servicenow.password:}")
    private String password;

    private WebClient snowClient;

    private WebClient getSnowClient() {
        if (snowClient == null) {
            snowClient = WebClient.builder()
                .baseUrl("https://%s.service-now.com/api/now".formatted(instance))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(h -> h.setBasicAuth(username, password))
                .build();
        }
        return snowClient;
    }

    /**
     * Create a service catalog item for a Business Role.
     * Users can then request this role via the ServiceNow portal.
     */
    public String createCatalogItem(String roleName, String description, String category, String approver) {
        if (!isConfigured()) {
            log.info("ServiceNow not configured — skipping catalog item for {}", roleName);
            return "demo-cat-item-id";
        }

        var body = Map.of(
            "name", "Request Role: " + roleName,
            "short_description", description,
            "category", category,
            "active", true,
            "delivery_time", "1 business day",
            "type", "item",
            "u_accessforge_role", roleName,
            "u_approval_group", approver
        );

        var response = getSnowClient()
            .post()
            .uri("/table/sc_cat_item")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        var result = (Map<String, Object>) response.get("result");
        String sysId = (String) result.get("sys_id");
        log.info("Created SNOW catalog item for {}: {}", roleName, sysId);
        return sysId;
    }

    /**
     * Create an access request (RITM) for a user to be assigned a Business Role.
     */
    public String createAccessRequest(String userId, String roleName, String justification) {
        if (!isConfigured()) {
            log.info("ServiceNow not configured — skipping access request for {}", userId);
            return "demo-ritm-id";
        }

        var body = Map.of(
            "requested_for", userId,
            "short_description", "Access Request: " + roleName,
            "description", justification,
            "u_accessforge_role", roleName,
            "u_request_type", "access_bundle_assignment",
            "urgency", "3",
            "priority", "3"
        );

        var response = getSnowClient()
            .post()
            .uri("/table/sc_req_item")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        var result = (Map<String, Object>) response.get("result");
        String ritmNumber = (String) result.get("number");
        log.info("Created SNOW RITM {} for user {} role {}", ritmNumber, userId, roleName);
        return ritmNumber;
    }

    /**
     * Close/fulfill a request item after provisioning is complete.
     */
    public void closeRequest(String ritmSysId, String closeNotes) {
        if (!isConfigured()) return;

        var body = Map.of(
            "state", "3",  // Closed Complete
            "close_notes", closeNotes
        );

        getSnowClient()
            .patch()
            .uri("/table/sc_req_item/%s".formatted(ritmSysId))
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        log.info("Closed RITM {}", ritmSysId);
    }

    /**
     * Get all pending access requests from ServiceNow.
     */
    public List<ServiceNowRequest> getPendingRequests() {
        if (!isConfigured()) return getDemoRequests();

        var response = getSnowClient()
            .get()
            .uri("/table/sc_req_item?sysparm_query=state=1^u_request_type=access_bundle_assignment&sysparm_limit=50")
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        var results = (List<Map<String, Object>>) response.get("result");
        if (results == null) return List.of();

        return results.stream()
            .map(r -> ServiceNowRequest.builder()
                .sysId((String) r.get("sys_id"))
                .number((String) r.get("number"))
                .requestedFor((String) r.get("requested_for"))
                .shortDescription((String) r.get("short_description"))
                .state((String) r.get("state"))
                .roleName((String) r.get("u_accessforge_role"))
                .build())
            .toList();
    }

    /**
     * Create an incident for governance issues (e.g., orphan SGs, compliance gaps).
     */
    public String createGovernanceIncident(String summary, String description, String assignmentGroup) {
        if (!isConfigured()) return "demo-inc-id";

        var body = Map.of(
            "short_description", "[AccessForge] " + summary,
            "description", description,
            "category", "Security",
            "subcategory", "IAM Governance",
            "assignment_group", assignmentGroup,
            "urgency", "2",
            "impact", "2"
        );

        var response = getSnowClient()
            .post()
            .uri("/table/incident")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        var result = (Map<String, Object>) response.get("result");
        String incNumber = (String) result.get("number");
        log.info("Created governance incident: {}", incNumber);
        return incNumber;
    }

    public boolean isConfigured() {
        return instance != null && !instance.isBlank()
            && username != null && !username.isBlank()
            && password != null && !password.isBlank();
    }

    private List<ServiceNowRequest> getDemoRequests() {
        return List.of(
            ServiceNowRequest.builder().sysId("s1").number("RITM0010042").requestedFor("U015").shortDescription("Request Role: Finance-Analyst-EMEA").state("Pending").roleName("Finance-Analyst-EMEA").build(),
            ServiceNowRequest.builder().sysId("s2").number("RITM0010043").requestedFor("U025").shortDescription("Request Role: HR-Specialist-Global").state("Pending").roleName("HR-Specialist-Global").build()
        );
    }

    @Builder
    public record ServiceNowRequest(String sysId, String number, String requestedFor, String shortDescription, String state, String roleName) {}
}
