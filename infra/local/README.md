# Local Infra

Spin up supporting services for local development via:
```bash
docker compose -f docker-compose.yml up -d
```
This starts Postgres 16 with credentials matching `application.yml`. Vector DB placeholders can be added as new services later.
