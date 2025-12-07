# Azure Infrastructure

Use the Bicep modules in `bicep/` to provision:
1. Azure OpenAI resource + deployments.
2. App Service plan + Web App hosting the Spring Boot API.
3. Cosmos DB / Postgres vector stores.

Deploy with:
```bash
az deployment sub create \
  --name ai-agent-platform \
  --location eastus \
  --template-file bicep/main.bicep \
  --parameters environment=dev
```
