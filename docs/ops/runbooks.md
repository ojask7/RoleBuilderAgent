# Runbook: Agent API

1. **Health Check** – `GET /actuator/health` or `GET /health/ping`.
2. **Restart** – `kubectl rollout restart deploy/agent-api` or re-deploy App Service slot.
3. **Config Update** – edit `application-prod.yml` overrides in KeyVault/App Config, trigger pipeline.
4. **Vector Store Refresh** – run ingestion workflow (see `scripts/load-sample-data.sh`).
