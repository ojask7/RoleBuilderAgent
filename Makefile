# =============================================================================
# RoleBuilderAgent — Makefile
# =============================================================================
# Quick reference:
#   make setup        — first-time setup (copy .env, start DB, run migrations)
#   make dev          — start DB + run Spring Boot locally (hot-reload)
#   make seed         — load sample IAM data into Postgres
#   make test         — run unit/integration tests
#   make run-docker   — build & run everything in Docker containers
#   make status       — check what's running and healthy
#   make teardown     — stop everything and remove volumes
# =============================================================================

SHELL := /bin/bash
COMPOSE := docker compose -f infra/local/docker-compose.yml
MVN := ./mvnw -B 2>/dev/null || mvn -B

.PHONY: help setup dev dev-stop seed test build run-docker status logs teardown \
        db-up db-migrate db-psql db-reset pgadmin smoke-test clean

# ---------------------------------------------------------------------------
# Default target
# ---------------------------------------------------------------------------
help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

# ---------------------------------------------------------------------------
# First-time setup
# ---------------------------------------------------------------------------
setup: ## First-time setup: copy .env, start DB, run migrations, load seed data
	@echo "==> Checking .env file..."
	@test -f .env || (cp .env.example .env && echo "    Created .env from .env.example — edit it with your Azure OpenAI keys")
	@echo "==> Starting Postgres..."
	@$(COMPOSE) up -d postgres
	@echo "==> Waiting for Postgres health check..."
	@$(COMPOSE) up flyway
	@echo "==> Loading sample data..."
	@bash scripts/load-sample-data.sh
	@echo ""
	@echo "==> Setup complete! Next steps:"
	@echo "    1. Edit .env with your AZURE_OPENAI_API_KEY"
	@echo "    2. Run: make dev"

# ---------------------------------------------------------------------------
# Local development (DB in Docker, app on host)
# ---------------------------------------------------------------------------
dev: db-up db-migrate ## Start DB + run Spring Boot locally with hot-reload
	@echo "==> Starting Spring Boot (Ctrl+C to stop)..."
	$(MVN) -pl backend/agent-api spring-boot:run \
		-Dspring-boot.run.profiles=dev \
		-Dspring-boot.run.jvmArguments="-Dspring.flyway.enabled=false"

dev-stop: ## Stop the local database
	@$(COMPOSE) stop postgres

# ---------------------------------------------------------------------------
# Database operations
# ---------------------------------------------------------------------------
db-up: ## Start Postgres in Docker
	@$(COMPOSE) up -d postgres
	@echo "==> Postgres running on localhost:$${POSTGRES_PORT:-5432}"

db-migrate: ## Run Flyway migrations via Docker
	@$(COMPOSE) up flyway

db-psql: ## Open psql shell to local database
	@docker exec -it rolebuilder-postgres psql -U $${POSTGRES_USER:-agent} -d $${POSTGRES_DB:-agentdb}

db-reset: ## Drop and recreate the database (destructive!)
	@echo "WARNING: This will destroy all data. Press Ctrl+C to cancel..."
	@sleep 3
	@docker exec rolebuilder-postgres psql -U $${POSTGRES_USER:-agent} -d postgres \
		-c "DROP DATABASE IF EXISTS $${POSTGRES_DB:-agentdb};" \
		-c "CREATE DATABASE $${POSTGRES_DB:-agentdb} OWNER $${POSTGRES_USER:-agent};"
	@$(COMPOSE) up flyway
	@echo "==> Database reset and migrations applied."

# ---------------------------------------------------------------------------
# Seed data
# ---------------------------------------------------------------------------
seed: ## Load sample IAM data (SGs, users, apps) into Postgres
	@bash scripts/load-sample-data.sh

# ---------------------------------------------------------------------------
# Test & Build
# ---------------------------------------------------------------------------
test: ## Run unit and integration tests
	$(MVN) -pl backend/agent-api verify

build: ## Build the JAR (skip tests)
	$(MVN) -pl backend/agent-api -am package -DskipTests

# ---------------------------------------------------------------------------
# Full Docker deployment
# ---------------------------------------------------------------------------
run-docker: ## Build and run everything in Docker (DB + migrations + API)
	@test -f .env || (cp .env.example .env && echo "Created .env — edit AZURE_OPENAI_API_KEY before using AI features")
	$(COMPOSE) --profile app up --build -d
	@echo ""
	@echo "==> All services starting. Check status with: make status"
	@echo "    API:     http://localhost:$${AGENT_API_PORT:-8080}/actuator/health"
	@echo "    Swagger: http://localhost:$${AGENT_API_PORT:-8080}/swagger-ui.html"

# ---------------------------------------------------------------------------
# Optional tools
# ---------------------------------------------------------------------------
pgadmin: ## Start pgAdmin web UI at http://localhost:5050
	$(COMPOSE) --profile tools up -d pgadmin
	@echo "==> pgAdmin at http://localhost:5050 (admin@local.dev / admin)"
	@echo "    Add server: host=postgres, port=5432, user=agent, password=agentpass"

# ---------------------------------------------------------------------------
# Ops
# ---------------------------------------------------------------------------
status: ## Show running containers and health status
	@echo "=== Containers ==="
	@docker ps --filter "name=rolebuilder-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
	@echo ""
	@echo "=== API Health ==="
	@curl -sf http://localhost:$${AGENT_API_PORT:-8080}/actuator/health 2>/dev/null \
		| python3 -m json.tool 2>/dev/null \
		|| echo "API not reachable (is it running?)"

logs: ## Tail logs from all containers
	$(COMPOSE) --profile app logs -f

smoke-test: ## Run quick API smoke test against localhost
	@echo "==> Health check..."
	@curl -sf http://localhost:$${AGENT_API_PORT:-8080}/actuator/health | python3 -m json.tool
	@echo ""
	@echo "==> Entitlement summary..."
	@curl -sf -u $${AGENT_API_USERNAME:-agent}:$${AGENT_API_PASSWORD:-agent-secret} \
		http://localhost:$${AGENT_API_PORT:-8080}/api/v1/entitlements/summary | python3 -m json.tool
	@echo ""
	@echo "==> Compliance dashboard..."
	@curl -sf -u $${AGENT_API_USERNAME:-agent}:$${AGENT_API_PASSWORD:-agent-secret} \
		http://localhost:$${AGENT_API_PORT:-8080}/api/v1/bundles/compliance/dashboard | python3 -m json.tool
	@echo ""
	@echo "==> Smoke test passed."

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------
clean: ## Remove build artifacts
	$(MVN) clean

teardown: ## Stop all containers and remove volumes (destructive!)
	@echo "WARNING: This removes all containers and data volumes. Press Ctrl+C to cancel..."
	@sleep 3
	$(COMPOSE) --profile app --profile tools down -v
	@echo "==> All containers and volumes removed."
