# Backend Architecture (Current State)

## 1. High-Level Overview
- **Backend framework:** Spring Boot (Java 21)
- **Persistence:** MySQL with JSON-document style storage
- **Migration/versioning:** Flyway
- **API style:** REST JSON under `/api`
- **Target:** Keep frontend API contract compatible with previous implementation

## 2. Main Components

### 2.1 API Layer
- Controller: `ApiController`
- Handles modules:
  - demo/status/reset
  - dashboard
  - projects
  - suppliers
  - catalog/products/categories
  - RFQs
  - quotes
  - price history
  - alerts/rules/events
  - quote compare
  - health/ready

### 2.2 Document Store Layer
- `SqlDocumentStore`
- Single-table JSON document persistence model (`documents`)
- Core operations:
  - upsert
  - find/findOne
  - deleteOne/deleteByQuery
  - updateByQuery
  - count
  - distinct
- Query operators supported in filtering logic include common patterns used by FE contract (`$regex`, `$in`, `$gte`)

### 2.3 Security Layer
- `AuthFilter`: reads Bearer token and sets request auth context
- `AuthTokenService`: token creation/validation (HMAC), with support for previous secrets (rotation window)
- `PasswordService`: bcrypt hashing + matching (with compatibility fallback)
- `SecurityPolicyGuard`: startup policy warnings for weak secrets/plaintext admin password
- RBAC model in controller:
  - `admin` required for mutating endpoints
  - read endpoints available with context
- Demo mode bypass remains intentionally enabled for iterative development

### 2.4 API Hardening
- Input validation for mutating endpoints (required fields, non-empty lists, numeric constraints)
- Unified error response through `ApiExceptionHandler`
- Request correlation and logging via `RequestLoggingFilter`
- Rate limiting via `RateLimitFilter`

### 2.5 Observability
- Spring Actuator enabled
- Prometheus endpoint exposed
- Custom business/guardrail metrics:
  - `constructiq_auth_login_total`
  - `constructiq_auth_token_total`
  - `constructiq_rate_limit_allowed_total`
  - `constructiq_rate_limit_blocked_total`

## 3. Data Architecture

### 3.1 Physical schema
- Table: `documents`
  - `collection_name`
  - `doc_id`
  - `org_id`
  - `json_data` (JSON)
  - timestamps
- Created and versioned through Flyway migration `V1__init_documents.sql`

### 3.2 Multi-tenancy
- Organization scoping with `org_id` included in read/write queries
- Demo org/user identifiers configurable from environment

## 4. Deployment Architecture
- Dockerized backend (`Dockerfile` multi-stage build)
- Compose stack (`docker-compose.yml`):
  - `mysql` service
  - `app` service
- Current host mappings:
  - backend: `8010 -> 8000`
  - mysql: `3307 -> 3306`
- Runtime environment configured through `.env` values

## 5. Current Operational Status
- Build/tests pass
- Flyway migrations run successfully on startup
- Critical API E2E flow validated on running stack
