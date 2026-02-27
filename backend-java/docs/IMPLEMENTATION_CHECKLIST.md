# Implementation Checklist

## Completed

### Backend migration and contract
- [x] Java Spring backend in place
- [x] MySQL persistence replacing MongoDB
- [x] API contract aligned to frontend expectations
- [x] FE-critical endpoint shape/pagination compatibility checks

### Data and schema
- [x] Flyway integrated
- [x] Versioned migration for `documents` table
- [x] Runtime schema bootstrap removed from store code

### Security and access
- [x] Token-based authentication
- [x] RBAC (`admin` for mutating routes)
- [x] Demo-mode access path retained for development
- [x] Password hashing service (bcrypt support)
- [x] Secret rotation support (`AUTH_TOKEN_PREVIOUS_SECRETS`)
- [x] Startup policy warnings for weak/default security settings

### Hardening and observability
- [x] Unified API error shape
- [x] Input validation on mutating APIs
- [x] Request correlation ID logging
- [x] Rate limiting
- [x] Prometheus metrics endpoint
- [x] Custom auth/rate-limit metrics

### Validation
- [x] Unit/contract tests passing
- [x] Build packaging passing
- [x] Critical API E2E flow passing

---

## Remaining (Next steps)

### CI/CD and release discipline
- [ ] Automated pipeline (test + build + image + deploy gates)
- [ ] Version tagging/release strategy
- [ ] Roll-forward/rollback automation in pipeline

### Security hardening to production standard
- [ ] Enforce hashed admin password via env (remove plaintext fallback policy path)
- [ ] Add token revocation/session invalidation strategy
- [ ] Add stricter CORS policy per environment
- [ ] Add security headers policy at gateway/reverse proxy level
- [ ] Add external security audit / penetration testing

### Reliability and operations
- [ ] Alerting rules on service/database/latency/error budget
- [ ] SLO/SLI definitions and dashboards
- [ ] Backup scheduling + restore rehearsal automation
- [ ] Load/performance test baseline and thresholds
- [ ] Failure-mode tests (DB outage, restart behavior, recovery)

### Quality expansion
- [ ] Broader integration tests with deterministic test fixtures
- [ ] Contract test pack versioned against frontend builds
- [ ] Data cleanup/archival strategy for long-running environments

---

## Current Estimate
- Production readiness estimate after latest validation: **~93%**
- This remains a technical readiness estimate, not a formal external certification.
