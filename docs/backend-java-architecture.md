# Arhitectura backend (stare curenta) - versiune RO

**Versiune document:** RO (Romana)

## 1. Vedere de ansamblu
- **Framework backend:** Spring Boot (Java 21)
- **Persistenta:** MySQL cu stocare tip document JSON
- **Migrare/versionare schema:** Flyway
- **Stil API:** REST JSON sub `/api`
- **Obiectiv:** pastrarea compatibilitatii contractului API cu frontend-ul existent

## 2. Componente principale

### 2.1 Strat API
- Controller principal: `ApiController`
- Module gestionate:
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

### 2.2 Strat stocare documente
- `SqlDocumentStore`
- Model de persistenta JSON intr-un singur tabel (`documents`)
- Operatii de baza:
  - upsert
  - find/findOne
  - deleteOne/deleteByQuery
  - updateByQuery
  - count
  - distinct
- Operatori de query suportati (utilizati de contractul frontend): `$regex`, `$in`, `$gte`

### 2.3 Strat securitate
- `AuthFilter`: citeste token Bearer si seteaza contextul de autentificare al request-ului
- `AuthTokenService`: creare/validare token (HMAC), cu suport pentru secrete anterioare (fereastra de rotatie)
- `PasswordService`: hash/parola cu bcrypt (plus fallback de compatibilitate)
- `SecurityPolicyGuard`: avertizari la startup pentru secrete slabe/parola admin in clar
- Model RBAC in controller:
  - `admin` necesar pentru endpoint-uri care modifica date
  - endpoint-uri de citire disponibile in functie de context
- Bypass-ul de demo mode ramane activ intentionat pentru dezvoltare iterativa

### 2.4 Intarire API
- Validare input pentru endpoint-uri mutabile (campuri obligatorii, liste non-goale, constrangeri numerice)
- Raspuns unificat de eroare prin `ApiExceptionHandler`
- Corelare request + logging prin `RequestLoggingFilter`
- Limitare de rata prin `RateLimitFilter`

### 2.5 Observabilitate
- Spring Actuator activ
- Endpoint Prometheus expus
- Metrici custom business/guardrail:
  - `constructiq_auth_login_total`
  - `constructiq_auth_token_total`
  - `constructiq_rate_limit_allowed_total`
  - `constructiq_rate_limit_blocked_total`

## 3. Arhitectura datelor

### 3.1 Schema fizica
- Tabel: `documents`
  - `collection_name`
  - `doc_id`
  - `org_id`
  - `json_data` (JSON)
  - timestamps
- Creat si versionat prin migrarea Flyway `V1__init_documents.sql`

### 3.2 Multi-tenancy
- Izolare pe organizatie prin `org_id` inclus in query-urile de citire/scriere
- Identificatorii demo org/user sunt configurabili din variabile de mediu

## 4. Arhitectura de deployment
- Backend containerizat (`Dockerfile` cu build multi-stage)
- Stack Docker Compose (`docker-compose.yml`):
  - serviciu `mysql`
  - serviciu `app`
- Mapari host curente:
  - backend: `8010 -> 8000`
  - mysql: `3307 -> 3306`
- Mediul runtime este configurat prin valori din `.env`

## 5. Stare operationala curenta
- Build/teste: trecute
- Migrarile Flyway ruleaza cu succes la startup
- Fluxul critic E2E API este validat pe stack-ul in rulare
