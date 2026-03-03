package com.yourorg.aiplatform.agentapi.integration.sailpoint;

import java.util.ArrayList;
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
 * SailPoint IdentityNow connector.
 * Reads entitlements and sources; pushes approved Access Bundles as roles.
 *
 * Uses SailPoint IdentityNow v3 API with OAuth2 client credentials.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SailPointClient {

    @Value("${accessforge.sailpoint.tenant:}")
    private String tenant;

    @Value("${accessforge.sailpoint.client-id:}")
    private String clientId;

    @Value("${accessforge.sailpoint.client-secret:}")
    private String clientSecret;

    private WebClient apiClient;

    private WebClient getApiClient() {
        if (apiClient == null) {
            String baseUrl = "https://%s.api.identitynow.com".formatted(tenant);
            apiClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        }
        return apiClient;
    }

    public String getAccessToken() {
        if (!isConfigured()) return "demo-token";

        String tokenUrl = "https://%s.api.identitynow.com/oauth/token".formatted(tenant);
        var response = WebClient.create()
            .post()
            .uri(tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("grant_type=client_credentials&client_id=%s&client_secret=%s"
                .formatted(clientId, clientSecret))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        return (String) response.get("access_token");
    }

    /**
     * List all entitlements from SailPoint sources.
     */
    public List<SailPointEntitlement> listEntitlements() {
        if (!isConfigured()) {
            log.info("SailPoint not configured — returning demo entitlements");
            return getDemoEntitlements();
        }

        String token = getAccessToken();
        List<SailPointEntitlement> results = new ArrayList<>();
        int offset = 0;
        int limit = 250;

        while (true) {
            var response = getApiClient()
                .get()
                .uri("/v3/entitlements?offset=%d&limit=%d".formatted(offset, limit))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block();

            if (response == null || response.isEmpty()) break;

            for (var ent : response) {
                var source = (Map<String, Object>) ent.get("source");
                results.add(SailPointEntitlement.builder()
                    .id((String) ent.get("id"))
                    .name((String) ent.get("name"))
                    .displayName((String) ent.get("displayName"))
                    .description((String) ent.get("description"))
                    .sourceName(source != null ? (String) source.get("name") : null)
                    .sourceId(source != null ? (String) source.get("id") : null)
                    .privileged(Boolean.TRUE.equals(ent.get("privileged")))
                    .build());
            }

            if (response.size() < limit) break;
            offset += limit;
        }

        log.info("Fetched {} entitlements from SailPoint", results.size());
        return results;
    }

    /**
     * List all sources (application connections) in SailPoint.
     */
    public List<SailPointSource> listSources() {
        if (!isConfigured()) return List.of();

        String token = getAccessToken();
        var response = getApiClient()
            .get()
            .uri("/v3/sources")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToFlux(Map.class)
            .collectList()
            .block();

        if (response == null) return List.of();

        return response.stream()
            .map(s -> SailPointSource.builder()
                .id((String) s.get("id"))
                .name((String) s.get("name"))
                .type((String) s.get("type"))
                .connectorType((String) s.get("connector"))
                .build())
            .toList();
    }

    /**
     * Create or update an Access Profile (IT Role equivalent) in SailPoint.
     */
    public String createAccessProfile(String name, String description, String sourceId, List<String> entitlementIds) {
        if (!isConfigured()) {
            log.info("SailPoint not configured — skipping access profile creation for {}", name);
            return "demo-profile-id";
        }

        String token = getAccessToken();
        var entitlementRefs = entitlementIds.stream()
            .map(id -> Map.of("id", id, "type", "ENTITLEMENT"))
            .toList();

        var body = Map.of(
            "name", name,
            "description", description,
            "source", Map.of("id", sourceId, "type", "SOURCE"),
            "entitlements", entitlementRefs,
            "requestable", true
        );

        var response = getApiClient()
            .post()
            .uri("/v3/access-profiles")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String profileId = (String) response.get("id");
        log.info("Created SailPoint access profile: {} -> {}", name, profileId);
        return profileId;
    }

    /**
     * Create a Role (Business Role equivalent) in SailPoint from access profiles.
     */
    public String createRole(String name, String description, String ownerId, List<String> accessProfileIds) {
        if (!isConfigured()) {
            log.info("SailPoint not configured — skipping role creation for {}", name);
            return "demo-role-id";
        }

        String token = getAccessToken();
        var profileRefs = accessProfileIds.stream()
            .map(id -> Map.of("id", id, "type", "ACCESS_PROFILE"))
            .toList();

        var body = Map.of(
            "name", name,
            "description", description,
            "owner", Map.of("id", ownerId, "type", "IDENTITY"),
            "accessProfiles", profileRefs,
            "requestable", true
        );

        var response = getApiClient()
            .post()
            .uri("/v3/roles")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String roleId = (String) response.get("id");
        log.info("Created SailPoint role: {} -> {}", name, roleId);
        return roleId;
    }

    public boolean isConfigured() {
        return tenant != null && !tenant.isBlank()
            && clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }

    private List<SailPointEntitlement> getDemoEntitlements() {
        return List.of(
            SailPointEntitlement.builder().id("e1").name("CH_SG_SAP_FI_STG_Read").displayName("SAP FI Staging Read").sourceName("Active Directory").privileged(false).build(),
            SailPointEntitlement.builder().id("e2").name("CH_SG_SAP_FI_PRD_Read").displayName("SAP FI Prod Read").sourceName("Active Directory").privileged(false).build(),
            SailPointEntitlement.builder().id("e3").name("SG_PowerBI_Finance_View").displayName("PowerBI Finance").sourceName("Active Directory").privileged(false).build(),
            SailPointEntitlement.builder().id("e4").name("SG_ITSM_SelfService").displayName("ITSM Portal").sourceName("Active Directory").privileged(false).build(),
            SailPointEntitlement.builder().id("e5").name("SG_HR_Workday_Read").displayName("Workday Read").sourceName("Active Directory").privileged(false).build()
        );
    }

    @Builder
    public record SailPointEntitlement(String id, String name, String displayName, String description, String sourceName, String sourceId, boolean privileged) {}

    @Builder
    public record SailPointSource(String id, String name, String type, String connectorType) {}
}
