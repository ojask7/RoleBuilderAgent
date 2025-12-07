package com.yourorg.aiplatform.agentapi.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class Kc27EvaluationService {

    public boolean isCompliant(String securityGroup, List<String> applicationServices) {
        return applicationServices.stream().anyMatch(app -> app.toLowerCase().contains("core"));
    }

    public double estimateConfidence(List<String> applicationServices, List<String> businessApplications) {
        int signals = applicationServices.size() + businessApplications.size();
        return Math.min(0.95, 0.4 + signals * 0.1);
    }
}
