# ConstructIQ - Procurement & Price Intelligence for Construction

## Original Problem Statement
Build a B2B SaaS MVP for "Procurement & Price Intelligence for Construction" - a platform for mid-size construction companies & real-estate developers. Core features include:
- Multi-tenant org model with Demo Mode (no login required)
- Procurement workflow (Projects, Suppliers, RFQs, Quotes)
- Price history per normalized SKU with charts
- Simple rule-based alerts
- Data import with manual entry + file upload

## User Personas
1. **Procurement Manager** - Creates RFQs, manages suppliers, tracks quotes
2. **Project Manager** - Oversees project procurement, views price trends
3. **Executive** - Dashboard overview, alert notifications

## Core Requirements (Static)
- Cookie-based auth (future) with Demo Mode for MVP
- Multi-tenant isolation by org_id
- RBAC: Admin, Procurement, Viewer roles
- PostgreSQL-ready schema (using MongoDB for MVP)
- Azure deployment ready

## Architecture
- **Frontend**: React + Tailwind CSS + Shadcn UI
- **Backend**: FastAPI (Python) with async endpoints
- **Database**: MongoDB (PostgreSQL migration path available)
- **Email**: Resend integration (placeholder for RFQ sending)
- **AI**: Emergent LLM Key for future quote parsing

## What's Been Implemented (January 2026)

### Backend (/app/backend/server.py)
- ✅ FastAPI app with all CRUD endpoints
- ✅ Demo Mode with auto-seeding (demo org, user, projects, suppliers, products)
- ✅ Organizations, Users, Projects, Suppliers models
- ✅ RFQ workflow: create, update, send to suppliers
- ✅ Quote ingestion with line items
- ✅ Catalog/Normalized Products management
- ✅ Price Points creation from quotes
- ✅ Alert Rules CRUD and evaluation logic
- ✅ Dashboard stats endpoint
- ✅ Health/Ready endpoints

### Frontend Pages
- ✅ Dashboard with KPIs and quick actions
- ✅ Suppliers list with CRUD
- ✅ Supplier detail with edit/delete
- ✅ Projects list with CRUD and status filtering
- ✅ Project detail with edit/delete
- ✅ RFQs list with status filtering
- ✅ RFQ Create wizard with items and supplier selection
- ✅ RFQ Detail with send functionality
- ✅ Quotes list with status filtering
- ✅ Quote Create with line items
- ✅ Quote Detail with product mapping
- ✅ Product Catalog with category filtering
- ✅ Price History with Recharts visualization
- ✅ Alerts with Rules and Events tabs

### UI/UX
- ✅ "Structural Integrity" theme (professional light theme)
- ✅ Manrope + Public Sans + JetBrains Mono fonts
- ✅ Safety Orange accent color
- ✅ Responsive sidebar navigation
- ✅ Demo Mode badge
- ✅ Reset Demo Data functionality
- ✅ Toast notifications via Sonner
- ✅ Server-side pagination
- ✅ Data tables with proper styling

## Prioritized Backlog

### P0 (Critical for MVP)
- [x] All core CRUD operations
- [x] RFQ workflow
- [x] Quote ingestion
- [x] Price history tracking
- [x] Alert rules

### P1 (High Priority)
- [ ] Quote comparison view (endpoint exists, UI needed)
- [ ] File upload for quote attachments
- [ ] AI-powered quote parsing (interface ready)
- [ ] Email templates for RFQ sending

### P2 (Medium Priority)
- [ ] Real authentication (cookie-based sessions)
- [ ] CSRF protection
- [ ] Supplier contacts management
- [ ] Purchase orders from quotes
- [ ] Export data to CSV/Excel

### P3 (Nice to Have)
- [ ] Market-wide price benchmarking
- [ ] Supplier performance metrics
- [ ] Mobile-responsive improvements
- [ ] Audit logging UI

## Next Action Items
1. Implement Quote Comparison view UI
2. Add file upload for quote attachments
3. Create email templates for RFQ
4. Add real authentication when moving to production
5. Implement CSRF protection for state-changing routes

## Environment Variables Required
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

## API Endpoints Reference
- GET /api/demo/status - Demo mode status
- POST /api/demo/reset - Reset demo data
- GET /api/dashboard/stats - Dashboard KPIs
- CRUD /api/projects - Projects management
- CRUD /api/suppliers - Suppliers management
- CRUD /api/catalog/products - Product catalog
- CRUD /api/rfqs - RFQs management
- POST /api/rfqs/{id}/send - Send RFQ to suppliers
- CRUD /api/quotes - Quotes management
- POST /api/quotes/{id}/map-item/{item_id} - Map quote item to product
- GET /api/price-history - Price history data
- GET /api/price-history/product/{id} - Product-specific history
- CRUD /api/alerts/rules - Alert rules
- GET/PUT /api/alerts/events - Alert events
