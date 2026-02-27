# ConstructIQ Backend (Java Spring)

Spring Boot migration of the original FastAPI backend, now using MySQL persistence.

## Run

```bash
mvn spring-boot:run
```

Default port: `8000`
Base path: `/api`

## Environment Variables

- `MYSQL_URL` (default: `jdbc:mysql://localhost:3306/procurement_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `MYSQL_USER` (default: `root`)
- `MYSQL_PASSWORD` (default: `root`)
- `DEMO_MODE` (default: `true`)
- `DEMO_ORG_ID` (default: `demo-org-001`)
- `DEMO_USER_ID` (default: `demo-user-001`)
- `CORS_ORIGINS` (default: `*`, comma-separated)
- `SENDER_EMAIL` (default: `onboarding@resend.dev`)
- `RESEND_API_KEY` (default: empty)

### Suggested `.env` template (you can fill values later)

```env
MYSQL_URL=jdbc:mysql://localhost:3306/procurement_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
MYSQL_USER=root
MYSQL_PASSWORD=your_mysql_password

DEMO_MODE=true
DEMO_ORG_ID=demo-org-001
DEMO_USER_ID=demo-user-001

CORS_ORIGINS=http://localhost:3000

SENDER_EMAIL=onboarding@resend.dev
RESEND_API_KEY=
```

## Implemented API Coverage

- Demo: `/demo/status`, `/demo/reset`
- Dashboard: `/dashboard/stats`
- Projects CRUD
- Suppliers CRUD
- Catalog products CRUD + categories
- RFQs CRUD + `/rfqs/{id}/send`
- Quotes CRUD + `/quotes/{id}/map-item/{itemId}` + `/quotes/compare`
- Price history endpoints
- Alerts rules/events endpoints
- Health/readiness endpoints

## Notes

- Email send in RFQ flow is currently mocked with logs. When you are ready, set `RESEND_API_KEY` and wire a real Resend client.
- Demo data seeding runs at startup in demo mode.
- API payload shape is kept compatible with frontend contract (`/api` routes and pagination fields).
