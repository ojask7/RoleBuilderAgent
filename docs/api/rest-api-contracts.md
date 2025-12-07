# REST API Contracts

## POST /api/agents/kc27/verify
Request:
```
{
  "securityGroup": "SG-BILLING-ADMINS",
  "description": "Billing admin permissions",
  "applicationServices": ["APP_CORE_BILLING"],
  "businessApplications": ["BA-Finance"],
  "owner": "iam@yourorg.com"
}
```
Response:
```
{
  "status": "COMPLIANT",
  "confidence": 0.87,
  "rationale": "..."
}
```

## POST /api/agents/sg/mapping
Request:
```
{
  "name": "SG-BILLING-ADMINS",
  "description": "Billing admin permissions"
}
```
Response:
```
{
  "securityGroup": "SG-BILLING-ADMINS",
  "suggestedApplicationService": "APP_CORE_BILLING",
  "suggestedBusinessApplication": "BA-DigitalIdentity",
  "owner": "iam@yourorg.com",
  "reasoning": "..."
}
```
