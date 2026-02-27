# ConstructionIQ

Full-stack procurement application with a React frontend and a Java Spring Boot backend.

## Current Stack

- **Frontend:** React
- **Backend:** Spring Boot (Java 21)
- **Database:** MySQL
- **Schema migrations:** Flyway
- **Observability:** Spring Actuator + Prometheus metrics endpoint
- **Deployment baseline:** Docker + Docker Compose (`app + mysql`)

## Repository Structure

- `frontend/` - React application
- `backend-java/` - Spring Boot API, MySQL persistence, security, migrations, tests

## Backend Features (Implemented)

- API contract compatibility aligned with frontend usage
- Multi-tenant scoping by `org_id`
- CRUD and workflows for:
  - projects
  - suppliers
  - catalog/products
  - RFQs
  - quotes
  - price history
  - alerts/rules/events
- Auth + RBAC:
  - token-based auth
  - admin guard on mutating endpoints
  - demo-mode access path preserved for iterative development
- Security hardening:
  - bcrypt password support
  - token secret rotation window (`AUTH_TOKEN_PREVIOUS_SECRETS`)
  - startup policy warnings for weak/default security values
- API hardening:
  - request validation for mutating routes
  - unified API error response
  - request correlation logging
  - rate limiting
- Observability:
  - `/actuator/health`
  - `/actuator/info`
  - `/actuator/prometheus`
  - custom metrics for auth and rate limiting

## Quick Start (Docker Compose)

### 1) Backend env file

Create `backend-java/.env` (or reuse existing) with your values.

### 2) Start backend stack

```bash
docker compose -f backend-java/docker-compose.yml up -d --build
```

### 3) Verify API

```bash
curl http://localhost:8010/api/health
curl http://localhost:8010/api/ready
curl http://localhost:8010/actuator/prometheus
```

Default compose host mappings:
- backend: `8010 -> 8000`
- mysql: `3307 -> 3306`

## Local Validation Commands

From `backend-java/`:

```bash
mvn -q test
mvn -q -DskipTests package
```

## Frontend Environment

For local frontend-to-backend integration:

- `frontend/.env`
  - `REACT_APP_BACKEND_URL=http://localhost:8010/api`

## API Base

- Base path: `/api`
- Example health endpoint: `GET /api/health`

## Documentation Index

Detailed backend docs are in:

- `backend-java/docs/README.md`
- `backend-java/docs/ARCHITECTURE.md`
- `backend-java/docs/tests-passed/TEST_RESULTS.md`
- `backend-java/docs/IMPLEMENTATION_CHECKLIST.md`

## Current Technical Status

- Build/tests passing
- Flyway migrations applied successfully on startup
- Critical API E2E flow validated on live stack
- Technical production-readiness estimate: **~93%**

## Important Notes

- Demo mode is intentionally kept active for development changes.
- Resend integration is intentionally not connected by default.
