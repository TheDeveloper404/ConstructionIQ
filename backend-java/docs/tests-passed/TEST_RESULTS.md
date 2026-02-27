# Test Results - Validation Report

## Scope
This document records the latest successful validation runs for the Java backend (`backend-java`) and API compatibility flows.

## Automated Validation

### Passed Tests (latest run)
- `mvn -q test` - **PASS**
- `mvn -q -DskipTests package` - **PASS**

### Failed / Not Passed Tests (latest run)
- **None**

### 1) Unit + Contract Tests
- Command: `mvn -q test`
- Result: **PASS**
- Notes:
  - API contract tests passed (`ApiControllerContractTest`)
  - Includes pagination/shape checks and error-path checks for FE-critical routes

### 2) Build Packaging
- Command: `mvn -q -DskipTests package`
- Result: **PASS**

## Runtime Smoke Checks (Docker Compose)
Environment:
- Backend URL: `http://localhost:8010`
- API base: `http://localhost:8010/api`
- MySQL: Docker service (compose)

### Health/Readiness
- `GET /api/health` -> `healthy` (**PASS**)
- `GET /api/ready` -> `ready` with DB connected (**PASS**)

### Failed / Not Passed Runtime Checks (latest run)
- **None**

### Critical E2E Flow
Executed flow:
1. Create Project
2. Create Supplier
3. Create Product
4. Create RFQ
5. Send RFQ
6. Create Quote #1
7. Create Quote #2
8. Verify alerts list

Latest result snapshot:
```json
{
  "health": "healthy",
  "ready": "ready",
  "rfq_status": "sent",
  "quote_ids": [
    "af4fdd4b-6deb-46be-9a27-b4ddab7ac08b",
    "8992be49-d386-4e12-931e-eca7a9744c67"
  ],
  "alert_count": 2
}
```

Status: **PASS**

## Observability Checks
- `GET /actuator/prometheus` exposed and reachable (**PASS**)
- Custom metrics present in runtime:
  - `constructiq_auth_login_total`
  - `constructiq_rate_limit_allowed_total`
  - `constructiq_auth_token_total`

## Conclusion
Current backend validation suite and critical E2E API flow are passing on the latest run.

## Final Status (latest run)
- Passed checks: **all listed above**
- Failed checks: **none**
