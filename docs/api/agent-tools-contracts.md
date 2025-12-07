# Agent Tool Contracts

| Tool | Input | Output | Notes |
|------|-------|--------|-------|
| SailPointTool | securityGroupName | List<ApplicationServiceCode> | Calls SailPoint Role Mining APIs. |
| CmdbTool | applicationServiceCode | Map{businessApplication, owner} | Proxy to ServiceNow REST. |
| AdGroupTool | securityGroupName | List<ownerEmail> | Microsoft Graph query for owners/members. |
