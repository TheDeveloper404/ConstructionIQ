# ConstructIQ Backend (Java Spring) - versiune RO

**Versiune document:** RO (Romana)

Migrare in Spring Boot a backend-ului initial FastAPI, cu persistenta in MySQL.

## Rulare

```bash
mvn spring-boot:run
```

Port implicit: `8000`
Cale de baza API: `/api`

## Variabile de mediu

- `MYSQL_URL` (implicit: `jdbc:mysql://localhost:3306/procurement_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `MYSQL_USER` (implicit: `root`)
- `MYSQL_PASSWORD` (implicit: `root`)
- `DEMO_MODE` (implicit: `true`)
- `DEMO_ORG_ID` (implicit: `demo-org-001`)
- `DEMO_USER_ID` (implicit: `demo-user-001`)
- `CORS_ORIGINS` (implicit: `*`, separat prin virgula)
- `SENDER_EMAIL` (implicit: `onboarding@resend.dev`)
- `RESEND_API_KEY` (implicit: gol)

### Model recomandat `.env`

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

## Acoperire API implementata

- Demo: `/demo/status`, `/demo/reset`
- Dashboard: `/dashboard/stats`
- CRUD proiecte
- CRUD furnizori
- CRUD produse catalog + categorii
- CRUD RFQ + `/rfqs/{id}/send`
- CRUD oferte + `/quotes/{id}/map-item/{itemId}` + `/quotes/compare`
- Endpoint-uri istoric preturi
- Endpoint-uri reguli/evenimente alerte
- Endpoint-uri health/readiness

## Note

- Trimiterea email in fluxul RFQ este momentan simulata prin loguri. Cand esti gata, seteaza `RESEND_API_KEY` si conecteaza un client Resend real.
- Seed-ul de date demo ruleaza la pornire in modul demo.
- Structura payload-urilor API este pastrata compatibila cu contractul frontend (`/api` si campurile de paginare).

## Documentatie backend extinsa (fuzionata)

Acest document include si rolul fostului index `backend-java/docs/README.md`.

Documente asociate in `docs/`:
- `backend-java-architecture.md` - arhitectura backend curenta
- `backend-java-implementation-checklist.md` - checklist implementare + ce mai ramane
- `backend-java-test-results.md` - rezultate validare (teste/build/smoke/E2E)

### Rezumat runtime
- Cale API: `/api`
- Host backend implicit in compose: `http://localhost:8010`
- Endpoint-uri actuator: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`
