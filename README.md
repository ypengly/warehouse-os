# WarehouseOS

A warehouse and inventory management system built around inventory correctness:
every stock change is transactional, row-locked, and recorded in an append-only
ledger.

> **Status: Phase 1 complete, Phase 2 in progress.** See
> [Roadmap](#roadmap) for exactly what is and is not implemented. Nothing in
> this repository is stubbed or mocked — features that are not listed as done
> simply are not there yet.

---

## Overview

WarehouseOS manages products, warehouses, bin locations, stock levels and the
movements between them. The design goal is that no code path — and no manual
`psql` session — can leave inventory in an inconsistent state.

Three defensive layers enforce that:

1. **Domain methods.** `Inventory` has no public quantity setters used by
   business code; callers go through `reserve`, `releaseReservation`,
   `consumeReservation`, `increaseOnHand` and `decreaseOnHand`.
2. **Database constraints.** `CHECK (reserved_quantity <= quantity_on_hand)`
   and non-negative checks on every quantity column.
3. **Row locks.** Mutating paths load inventory with
   `SELECT ... FOR UPDATE` (`InventoryRepository.findByIdForUpdate`) with a 5s
   lock timeout, so concurrent reservations serialise rather than race.

`stock_movements` and `audit_logs` are append-only, enforced by a PostgreSQL
trigger that raises on `UPDATE` and `DELETE`.

## Architecture

```
Controller  →  DTO + Bean Validation  →  Service (@Transactional, @PreAuthorize)
            →  Repository  →  PostgreSQL
```

JPA entities never cross the HTTP boundary; every endpoint speaks in DTOs.
Business rules live in services, not controllers.

```
backend/src/main/java/com/warehouseos/
├── config/          AppProperties, OpenAPI
├── security/        JWT filter, SecurityConfig, principal, entry point
├── controller/      thin HTTP layer
├── service/         business logic and transactions
├── repository/      Spring Data + locking queries
├── entity/          JPA model
├── dto/             request/response records
├── mapper/          entity → DTO
├── exception/       error codes, ApiError, global handler
├── specification/   Criteria API predicates for search
└── seed/            development-only data loader
```

## Technology stack

Java 21 · Spring Boot 3.3 · Spring Web / Data JPA / Security · PostgreSQL 16 ·
Flyway · JWT (jjwt) · Lombok · springdoc-openapi · JUnit 5 · Testcontainers ·
Docker Compose.

## Database schema

The full schema ships in a single migration,
`backend/src/main/resources/db/migration/V1__core_schema.sql`, covering identity,
catalogue, locations, inventory, inbound, transfers, counting, picking,
notifications and audit. Later phases add behaviour, not new tables.

Details worth knowing:

- `inventory` uniqueness uses **two partial unique indexes** rather than one
  constraint, because PostgreSQL treats `NULL` location as distinct — without
  this you can silently create duplicate stock rows for unlocated inventory.
- `refresh_tokens` stores a **SHA-256 hash**, never the token itself.
- `stock_transfers` has a `CHECK` that source and destination differ.
- `purchase_order_items` has a `CHECK` preventing over-receipt.

## Environment variables

| Variable | Purpose | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/warehouseos` |
| `DATABASE_USERNAME` | DB user | `warehouseos` |
| `DATABASE_PASSWORD` | DB password | `warehouseos` |
| `JWT_SECRET` | Base64, ≥32 bytes | dev fallback (**override in deployment**) |
| `JWT_ACCESS_TTL` | Access token lifetime (ISO-8601) | `PT15M` |
| `JWT_REFRESH_TTL` | Refresh token lifetime | `P7D` |
| `SEED_ENABLED` | Load development seed data | `true` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins | `http://localhost:5173` |

## Docker setup

```bash
cp .env.example .env
openssl rand -base64 48        # paste into JWT_SECRET
docker compose up --build
```

API: <http://localhost:8080> · Swagger UI: <http://localhost:8080/swagger-ui.html>

## Local development

```bash
docker compose up -d postgres
cd backend
JWT_SECRET=$(openssl rand -base64 48) ./mvnw spring-boot:run
```

Flyway applies migrations on startup; Hibernate runs with `ddl-auto: validate`,
so a mismatch between entities and schema fails fast at boot rather than
corrupting data later.

## Test commands

```bash
cd backend
./mvnw test                                  # unit + integration
./mvnw test -Dtest=AuthFlowIntegrationTest   # single class
```

Integration tests start a real PostgreSQL container via Testcontainers, because
the invariants being tested are database-level. Docker must be running.

## Development credentials

**Development only.** Created by `DevDataSeeder` and skipped when
`SEED_ENABLED=false`. Never deploy with these present.

| Username | Role | Password |
|---|---|---|
| `admin` | ADMIN | `Warehouse123!` |
| `manager` | MANAGER | `Warehouse123!` |
| `staff` | STAFF | `Warehouse123!` |

Seed data also includes 2 warehouses, 13 bin locations, 3 categories,
2 suppliers and 22 products with stock deliberately spread across in-stock,
low-stock and out-of-stock states so dashboards have something real to show.

## API documentation

Interactive docs at `/swagger-ui.html`; the OpenAPI document at
`/v3/api-docs`. Click **Authorize** and paste an access token from
`POST /api/auth/login`.

### Implemented endpoints

```
POST   /api/auth/register          public — always creates a STAFF account
POST   /api/auth/login             public
POST   /api/auth/refresh           public — rotates the refresh token
POST   /api/auth/logout            revokes all refresh tokens
GET    /api/auth/me

GET    /api/products               ?q=&categoryId=&brand=&active=&page=&size=&sort=
GET    /api/products/{id}
GET    /api/products/sku/{sku}
GET    /api/products/barcode/{barcode}
POST   /api/products               ADMIN, MANAGER
PUT    /api/products/{id}          ADMIN, MANAGER
DELETE /api/products/{id}          ADMIN, MANAGER — deactivates, never deletes
POST   /api/products/{id}/activate ADMIN, MANAGER

GET    /api/warehouses
GET    /api/warehouses/{id}
POST   /api/warehouses             ADMIN
PUT    /api/warehouses/{id}        ADMIN
PATCH  /api/warehouses/{id}/status ADMIN
```

### Error format

Every failure — validation, auth, business rule, lock conflict — returns the
same shape:

```json
{
  "timestamp": "2026-02-14T09:31:22.104Z",
  "status": 409,
  "error": "INSUFFICIENT_STOCK",
  "message": "Not enough stock available for LAPTOP-001: requested 5, available 2",
  "path": "/api/transfers",
  "details": { "sku": "LAPTOP-001", "requested": 5, "available": 2 }
}
```

## Example workflows

**Log in and list low-stock candidates**

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"manager","password":"Warehouse123!"}' | jq -r .accessToken)

curl -s localhost:8080/api/products?q=laptop -H "Authorization: Bearer $TOKEN" | jq
```

**Role enforcement is server-side**

```bash
STAFF=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"staff","password":"Warehouse123!"}' | jq -r .accessToken)

curl -s -X POST localhost:8080/api/warehouses -H "Authorization: Bearer $STAFF" \
  -H 'Content-Type: application/json' -d '{"code":"WH-009","name":"Nope"}'
# → 403 ACCESS_DENIED
```

## Security notes

- BCrypt (strength 12); password hashes are absent from every DTO by
  construction, not by annotation.
- Login failures are logged without the attempted password and return an
  identical message whether the user exists or not.
- Refresh tokens rotate on use; replaying a consumed token revokes the entire
  token family for that user, which is the standard response to suspected theft.
- Roles are re-read from the database on each request rather than trusted from
  the JWT, so revoking a role takes effect immediately.
- Self-registration cannot produce a privileged account.
- CSRF is disabled deliberately: the API is stateless and authenticated by a
  bearer token browsers do not attach automatically.

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| 1 | Boot, PostgreSQL, Flyway, JWT auth, users/roles, error handling, Docker | **Done** |
| 2 | Products, categories, warehouses, locations | Products + warehouses done; categories, locations, suppliers pending |
| 3 | Inventory service, stock movements, goods receiving | Schema + entities + locking queries in place; services pending |
| 4 | Transfers, reservations, concurrency tests | Pending |
| 5 | Purchase orders, counting, picking | Pending |
| 6 | Dashboard, reports, notifications, audit logs | Pending |
| 7 | React frontend, full test suite | Pending |

## Known gaps

- The backend has not yet been compiled in this environment (no Maven registry
  access during authoring); run `./mvnw clean verify` first.
- No Maven wrapper is committed yet — use a local `mvn` or generate one with
  `mvn wrapper:wrapper`.
- The `frontend/` module has not been started.
