package com.yourorg.aiplatform.agentapi.api;

import com.yourorg.aiplatform.agentapi.integration.azuread.AzureADConnector;
import com.yourorg.aiplatform.agentapi.integration.sailpoint.SailPointClient;
import com.yourorg.aiplatform.agentapi.integration.servicenow.ServiceNowClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final AzureADConnector azureADConnector;
    private final SailPointClient sailPointClient;
    private final ServiceNowClient serviceNowClient;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getIntegrationStatus() {
        return ResponseEntity.ok(Map.of(
            "azureAD", Map.of(
                "configured", azureADConnector.isConfigured(),
                "status", azureADConnector.isConfigured() ? "connected" : "not_configured"
            ),
            "sailpoint", Map.of(
                "configured", sailPointClient.isConfigured(),
                "status", sailPointClient.isConfigured() ? "connected" : "not_configured"
            ),
            "serviceNow", Map.of(
                "configured", serviceNowClient.isConfigured(),
                "status", serviceNowClient.isConfigured() ? "connected" : "not_configured"
            ),
            "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/azure-ad/sync")
    public ResponseEntity<Map<String, Object>> syncAzureAD() {
        var groups = azureADConnector.listSecurityGroups();
        return ResponseEntity.ok(Map.of(
            "source", "Azure AD",
            "groupsFound", groups.size(),
            "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/sailpoint/sync")
    public ResponseEntity<Map<String, Object>> syncSailPoint() {
        var entitlements = sailPointClient.listEntitlements();
        return ResponseEntity.ok(Map.of(
            "source", "SailPoint IdentityNow",
            "entitlementsFound", entitlements.size(),
            "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/servicenow/catalog-item")
    public ResponseEntity<Map<String, Object>> createCatalogItem(@RequestBody Map<String, String> request) {
        String sysId = serviceNowClient.createCatalogItem(
            request.get("roleName"),
            request.get("description"),
            request.getOrDefault("category", "Access Management"),
            request.getOrDefault("approver", "IAM Governance")
        );
        return ResponseEntity.ok(Map.of("sysId", sysId, "status", "created"));
    }

    @GetMapping("/servicenow/pending-requests")
    public ResponseEntity<List<ServiceNowClient.ServiceNowRequest>> getPendingRequests() {
        return ResponseEntity.ok(serviceNowClient.getPendingRequests());
    }
}
