#!/usr/bin/env bash
set -euo pipefail

pushd "$(dirname "$0")/.." >/dev/null

if [ -f infra/local/docker-compose.yml ]; then
  echo "[dev-run] Starting local infra via docker compose"
  docker compose -f infra/local/docker-compose.yml up -d
fi

echo "[dev-run] Launching Spring Boot application"
mvn -pl backend/agent-api spring-boot:run
