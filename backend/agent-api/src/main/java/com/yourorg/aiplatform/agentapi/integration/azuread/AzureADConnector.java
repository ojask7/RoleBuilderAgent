package com.yourorg.aiplatform.agentapi.integration.azuread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Azure AD / Entra ID connector using Microsoft Graph API.
 * Reads security groups, memberships, and user attributes.
 *
 * Required Azure App Registration permissions:
 *   - Directory.Read.All
 *   - Group.Read.All
 *   - User.Read.All
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AzureADConnector {

    @Value("${accessforge.azure-ad.tenant-id:}")
    private String tenantId;

    @Value("${accessforge.azure-ad.client-id:}")
    private String clientId;

    @Value("${accessforge.azure-ad.client-secret:}")
    private String clientSecret;

    @Value("${accessforge.azure-ad.graph-endpoint:https://graph.microsoft.com/v1.0}")
    private String graphEndpoint;

    private WebClient graphClient;

    private WebClient getGraphClient() {
        if (graphClient == null) {
            graphClient = WebClient.builder()
                .baseUrl(graphEndpoint)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        }
        return graphClient;
    }

    /**
     * Fetch an OAuth2 token from Azure AD using client credentials flow.
     */
    public String getAccessToken() {
        if (tenantId.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            log.warn("Azure AD credentials not configured — running in demo mode");
            return "demo-token";
        }

        String tokenUrl = "https://login.microsoftonline.com/%s/oauth2/v2.0/token".formatted(tenantId);

        var response = WebClient.create()
            .post()
            .uri(tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("client_id=%s&client_secret=%s&scope=https://graph.microsoft.com/.default&grant_type=client_credentials"
                .formatted(clientId, clientSecret))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        return (String) response.get("access_token");
    }

    /**
     * List all security groups from Azure AD.
     * Handles pagination via @odata.nextLink.
     */
    public List<AzureADGroup> listSecurityGroups() {
        if (tenantId.isBlank()) {
            log.info("Azure AD not configured — returning demo data");
            return getDemoGroups();
        }

        String token = getAccessToken();
        List<AzureADGroup> allGroups = new ArrayList<>();
        String url = "/groups?$filter=securityEnabled eq true&$select=id,displayName,description,createdDateTime,mail&$top=100";

        while (url != null) {
            var response = getGraphClient()
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            var values = (List<Map<String, Object>>) response.get("value");
            if (values != null) {
                for (var group : values) {
                    allGroups.add(AzureADGroup.builder()
                        .id((String) group.get("id"))
                        .displayName((String) group.get("displayName"))
                        .description((String) group.get("description"))
                        .mail((String) group.get("mail"))
                        .build());
                }
            }

            url = (String) response.get("@odata.nextLink");
            if (url != null) {
                url = url.replace(graphEndpoint, "");
            }
        }

        log.info("Fetched {} security groups from Azure AD", allGroups.size());
        return allGroups;
    }

    /**
     * Get members of a specific security group.
     */
    public List<AzureADUser> getGroupMembers(String groupId) {
        if (tenantId.isBlank()) return List.of();

        String token = getAccessToken();
        var response = getGraphClient()
            .get()
            .uri("/groups/%s/members?$select=id,displayName,userPrincipalName,department,jobTitle".formatted(groupId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        var values = (List<Map<String, Object>>) response.get("value");
        if (values == null) return List.of();

        return values.stream()
            .map(u -> AzureADUser.builder()
                .id((String) u.get("id"))
                .displayName((String) u.get("displayName"))
                .userPrincipalName((String) u.get("userPrincipalName"))
                .department((String) u.get("department"))
                .jobTitle((String) u.get("jobTitle"))
                .build())
            .toList();
    }

    /**
     * Get all group memberships for a specific user.
     */
    public List<String> getUserGroupMemberships(String userId) {
        if (tenantId.isBlank()) return List.of();

        String token = getAccessToken();
        var response = getGraphClient()
            .get()
            .uri("/users/%s/memberOf?$select=id,displayName".formatted(userId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        var values = (List<Map<String, Object>>) response.get("value");
        if (values == null) return List.of();

        return values.stream()
            .map(g -> (String) g.get("displayName"))
            .collect(Collectors.toList());
    }

    public boolean isConfigured() {
        return !tenantId.isBlank() && !clientId.isBlank() && !clientSecret.isBlank();
    }

    private List<AzureADGroup> getDemoGroups() {
        return List.of(
            AzureADGroup.builder().id("g1").displayName("CH_SG_SAP_FI_STG_Read").description("SAP FI staging read").build(),
            AzureADGroup.builder().id("g2").displayName("CH_SG_SAP_FI_PRD_Read").description("SAP FI prod read").build(),
            AzureADGroup.builder().id("g3").displayName("SG_PowerBI_Finance_View").description("PowerBI Finance").build(),
            AzureADGroup.builder().id("g4").displayName("SG_ITSM_SelfService").description("ITSM portal").build(),
            AzureADGroup.builder().id("g5").displayName("SG_Unknown_Legacy_42").description(null).build()
        );
    }

    @Builder
    public record AzureADGroup(String id, String displayName, String description, String mail) {}

    @Builder
    public record AzureADUser(String id, String displayName, String userPrincipalName, String department, String jobTitle) {}
}
