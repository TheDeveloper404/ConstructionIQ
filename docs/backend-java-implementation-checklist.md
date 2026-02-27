# Checklist implementare backend - versiune RO

**Versiune document:** RO (Romana)

## Finalizat

### Migrare backend si contract API
- [x] Backend Java Spring implementat
- [x] Persistenta MySQL inlocuieste MongoDB
- [x] Contract API aliniat la asteptarile frontend
- [x] Verificari de compatibilitate pentru shape/paginare pe endpoint-uri critice FE

### Date si schema
- [x] Flyway integrat
- [x] Migrare versionata pentru tabelul `documents`
- [x] Initializarea runtime a schemei scoasa din codul de store

### Securitate si acces
- [x] Autentificare pe baza de token
- [x] RBAC (`admin` pentru rute care modifica date)
- [x] Cale de acces demo mode pastrata pentru dezvoltare
- [x] Serviciu de hash pentru parole (suport bcrypt)
- [x] Suport pentru rotatie secrete (`AUTH_TOKEN_PREVIOUS_SECRETS`)
- [x] Avertizari la startup pentru setari de securitate slabe/implicite

### Hardening si observabilitate
- [x] Format unificat pentru erori API
- [x] Validare input pe API-uri mutabile
- [x] Logging cu request correlation ID
- [x] Rate limiting
- [x] Endpoint metrici Prometheus
- [x] Metrici custom pentru auth/rate-limit

### Validare
- [x] Teste unitare/contract trecute
- [x] Build packaging trecut
- [x] Flux critic E2E API trecut

---

## Ramane de facut (urmatorii pasi)

### Disciplina CI/CD si release
- [ ] Pipeline automatizat (test + build + image + gating deploy)
- [ ] Strategie versionare/tagging release
- [ ] Automatizare roll-forward/rollback in pipeline

### Hardening de securitate pentru productie
- [ ] Impunere parola admin hash-uita prin env (eliminare fallback plaintext)
- [ ] Strategie de revocare token/invalide sesiuni
- [ ] Politica CORS mai stricta per mediu
- [ ] Politica security headers la nivel gateway/reverse proxy
- [ ] Audit extern de securitate / penetration testing

### Fiabilitate si operare
- [ ] Reguli de alertare pe serviciu/baza de date/latenta/error budget
- [ ] Definitii SLO/SLI si dashboard-uri
- [ ] Programare backup + automatizare exercitiu restore
- [ ] Baseline + praguri pentru teste de incarcare/performanta
- [ ] Teste failure-mode (cadere DB, restart, recuperare)

### Extindere calitate
- [ ] Teste de integrare mai extinse cu fixture-uri deterministe
- [ ] Pachet de contract tests versionat fata de build-urile frontend
- [ ] Strategie de curatare/arhivare date pentru medii de lunga durata

---

## Estimare curenta
- Estimare de pregatire pentru productie dupa ultima validare: **~93%**
- Este o estimare de pregatire tehnica, nu o certificare externa formala.
