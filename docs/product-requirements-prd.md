# ConstructIQ - Procurement si inteligenta de preturi pentru constructii (versiune RO)

**Versiune document:** RO (Romana)

## Problema initiala
Construirea unui MVP B2B SaaS pentru "Procurement si Price Intelligence in constructii" - o platforma pentru companii medii de constructii si dezvoltatori imobiliari. Functionalitati de baza:
- Model multi-tenant pe organizatii, cu Demo Mode (fara autentificare)
- Flux de achizitii (Proiecte, Furnizori, RFQ-uri, Oferte)
- Istoric de pret pe SKU normalizat, cu grafice
- Alerte simple pe baza de reguli
- Import de date prin introducere manuala + upload fisiere

## Persona utilizatori
1. **Manager Achizitii** - creeaza RFQ-uri, gestioneaza furnizori, urmareste ofertele
2. **Manager Proiect** - supravegheaza achizitiile pe proiect, urmareste trenduri de pret
3. **Executiv** - vedere de ansamblu in dashboard, notificari de alerta

## Cerinte de baza (statice)
- Autentificare pe cookie-uri (in viitor), cu Demo Mode pentru MVP
- Izolare multi-tenant prin `org_id`
- RBAC: roluri Admin, Procurement, Viewer
- Schema pregatita pentru PostgreSQL (MVP ruleaza pe MongoDB)
- Pregatit pentru deployment in Azure

## Arhitectura
- **Frontend**: React + Tailwind CSS + Shadcn UI
- **Backend**: FastAPI (Python), endpoint-uri async
- **Baza de date**: MongoDB (cu ruta de migrare spre PostgreSQL)
- **Email**: integrare Resend (placeholder pentru trimitere RFQ)
- **AI**: cheie Emergent LLM pentru parsare oferte in viitor

## Ce este implementat (ianuarie 2026)

### Backend (/app/backend/server.py)
- ✅ Aplicatie FastAPI cu toate endpoint-urile CRUD
- ✅ Demo Mode cu auto-seeding (organizatie demo, user, proiecte, furnizori, produse)
- ✅ Modele pentru Organizations, Users, Projects, Suppliers
- ✅ Flux RFQ: creare, actualizare, trimitere catre furnizori
- ✅ Ingestie oferte cu line items
- ✅ Management catalog/produse normalizate
- ✅ Creare price points din oferte
- ✅ CRUD reguli de alerta + logica de evaluare
- ✅ Endpoint pentru statistici dashboard
- ✅ Endpoint-uri health/ready

### Pagini frontend
- ✅ Dashboard cu KPI-uri si actiuni rapide
- ✅ Lista furnizori cu CRUD
- ✅ Detaliu furnizor cu editare/stergere
- ✅ Lista proiecte cu CRUD si filtrare dupa status
- ✅ Detaliu proiect cu editare/stergere
- ✅ Lista RFQ-uri cu filtrare dupa status
- ✅ Wizard creare RFQ cu item-uri si selectie furnizori
- ✅ Detaliu RFQ cu functionalitate de trimitere
- ✅ Lista oferte cu filtrare dupa status
- ✅ Creare oferta cu line items
- ✅ Detaliu oferta cu mapare produs
- ✅ Catalog produse cu filtrare pe categorii
- ✅ Istoric de pret cu vizualizare Recharts
- ✅ Alerte cu tab-uri Rules si Events

### UI/UX
- ✅ Tema "Structural Integrity" (tema luminoasa, profesionala)
- ✅ Fonturi Manrope + Public Sans + JetBrains Mono
- ✅ Culoare de accent Safety Orange
- ✅ Navigare in sidebar responsive
- ✅ Badge Demo Mode
- ✅ Functionalitate resetare date demo
- ✅ Notificari toast prin Sonner
- ✅ Paginare server-side
- ✅ Tabele de date cu stilizare corecta

## Backlog prioritizat

### P0 (critic pentru MVP)
- [x] Toate operatiile CRUD de baza
- [x] Flux RFQ
- [x] Ingestie oferte
- [x] Urmarire istoric pret
- [x] Reguli de alerta

### P1 (prioritate ridicata)
- [ ] UI pentru comparare oferte (endpoint exista)
- [ ] Upload fisiere pentru atasamente oferte
- [ ] Parsare oferte asistata AI (interfata pregatita)
- [ ] Template-uri email pentru trimitere RFQ

### P2 (prioritate medie)
- [ ] Autentificare reala (sesiuni pe cookie)
- [ ] Protectie CSRF
- [ ] Management contacte furnizori
- [ ] Comenzi de achizitie din oferte
- [ ] Export date CSV/Excel

### P3 (nice to have)
- [ ] Benchmarking de pret la nivel de piata
- [ ] Metrici de performanta furnizori
- [ ] Imbunatatiri responsive pe mobil
- [ ] UI pentru audit logging

## Urmatoarele actiuni
1. Implementare UI pentru comparare oferte
2. Adaugare upload de fisiere pentru atasamente oferte
3. Creare template-uri email pentru RFQ
4. Adaugare autentificare reala pentru productie
5. Implementare protectie CSRF pentru rute care modifica stare

## Variabile de mediu necesare
```
MONGO_URL=mongodb://localhost:27017
DB_NAME=procurement_db
DEMO_MODE=true
RESEND_API_KEY=re_your_api_key_here
SENDER_EMAIL=onboarding@resend.dev
# LLM_API_KEY=sk-your-api-key...
SESSION_SECRET=your_session_secret
CSRF_SECRET=your_csrf_secret
```

## Referinta endpoint-uri API
- GET /api/demo/status - stare Demo Mode
- POST /api/demo/reset - resetare date demo
- GET /api/dashboard/stats - KPI-uri dashboard
- CRUD /api/projects - management proiecte
- CRUD /api/suppliers - management furnizori
- CRUD /api/catalog/products - catalog produse
- CRUD /api/rfqs - management RFQ-uri
- POST /api/rfqs/{id}/send - trimitere RFQ catre furnizori
- CRUD /api/quotes - management oferte
- POST /api/quotes/{id}/map-item/{item_id} - mapare item oferta la produs
- GET /api/price-history - date istoric pret
- GET /api/price-history/product/{id} - istoric pe produs
- CRUD /api/alerts/rules - reguli de alerta
- GET/PUT /api/alerts/events - evenimente alerte
