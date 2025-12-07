# UNO Role Builder AI

UNO Role Builder AI is a high-level reference platform designed to modernize Identity Governance using Spring Boot 3, Spring AI, and Azure OpenAI. 

The platform provides a unified foundation for building intelligent IAM agents that automate reasoning across AD Security Groups (SGs), SailPoint roles, and CMDB application metadata. It supports core use cases such as mapping SGs to Application Services (AS) and Business Applications (BA), inferring ownership, and preparing structured information needed for governance workflows.

At the heart of the system is a data-fusion and agentic reasoning layer that solves a long-standing IAM challenge: 
- inconsistent and unmanaged SG landscapes leading to missing ownership, missing approvals, 
- incomplete AS/BA linkage, and ultimately KC27 non-compliance.

 ## Key Capabilities
 
 - ingests organisational data from AD, SailPoint, CMDB, and MDT sources,
  -enabling AI agents to suggest role relationships, 
  -propose IT/Business Roles, highlight compliance gaps, 
  -create audit-ready evidence.

This repository contains the Spring Boot backend, agent pipeline scaffolding, infrastructure templates, and documentation required to extend the platform into enterprise environments. It acts as a starting point for integrating real connectors (SailPoint, CMDB, AD), enabling vector-based retrieval, and evolving towards a fully automated IAM role modelling and compliance engine.

## Technical  Stack 
- Spring Boot 3.x, Java 21, and Spring AI Azure OpenAI starter for conversational agents.
- Clean project layout with backend service, infra-as-code, docs, ops automation, and experiment artifacts.
- Agent pipelines that combine prompts, vector search, and domain services (SailPoint, CMDB, AD) — currently stubbed to simplify local development.
- Ready-to-extend infra (Bicep, Helm, GitHub Actions) for Azure App Service and Kubernetes deployments.

## Getting Started
1. **Prerequisites**: Java 21, Maven 3.9+, Docker (for local Postgres), and Azure OpenAI credentials.
2. **Configuration**: Export Azure variables or override entries in `backend/agent-api/src/main/resources/application.yml`.
3. **Run infrastructure** (optional): `docker compose -f infra/local/docker-compose.yml up -d`.
4. **Launch the backend**: `mvn -pl backend/agent-api spring-boot:run` or run from the repo root with `scripts/dev-run-local.sh`.
5. **Call the APIs**: `POST /api/agents/kc27/verify` and `POST /api/agents/sg/mapping` for stubbed agent responses.

## Project Layout (excerpt)
- `backend/agent-api` – Spring Boot agent API service.
- `docs/` – product vision, personas, specs, and architecture notes.
- `infra/` – Azure Bicep modules, local docker-compose, and Kubernetes manifests.
- `ops/` – GitHub workflows, Helm chart stub, and monitoring assets.
- `data/` – placeholder datasets, vector store notes, and Flyway migrations.
- `experiments/` – prompt iterations, evaluation templates, and a starter notebook.
- `scripts/` – helper scripts for local dev, sample data loading, and OpenAPI export.

## Integrations & Next Steps
The SailPoint, CMDB, and Active Directory connectors are currently mocked via tool classes inside `agent/tools`. Replace their implementations with real adapters when wiring to enterprise systems. Extend `VectorStoreConfig` to back embeddings with Azure Cosmos DB or Postgres/pgvector, and tailor prompts plus RAG policies to your data estate.
