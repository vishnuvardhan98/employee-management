# PeopleDesk — Employee Management

A full-stack employee management application built for the Application Developer skill assessment.

| Layer     | Technology |
| --------- | ---------- |
| Frontend  | Angular 19 (standalone components, signals, Reactive Forms), Angular Material |
| Backend   | Spring Boot 3.4 (Java 21), Spring Security, Spring Data JPA / Hibernate, Bean Validation |
| Database  | MySQL 8.4, schema managed by Flyway |
| Auth      | JWT access tokens (HS256, 15 min) + rotating refresh tokens, BCrypt password hashes |
| Reports   | Apache POI (Excel), OpenPDF (PDF; LGPL fork of iText) |
| Packaging | Multi-stage Dockerfile + docker compose |

```
Browser ──► Angular SPA ──HTTP/JSON (Bearer JWT)──► Spring Boot REST API ──JPA──► MySQL
            (dev: :4200, proxied)                   (:8080, also serves the SPA in Docker)   (:3306)
```

## Quick start (Docker — recommended)

Requires Docker Desktop (or Docker Engine with the compose plugin).

```sh
cp .env.example .env          # optional: set JWT_SECRET and passwords
docker compose up --build
```

Open **http://localhost:8080** and sign in:

| Username | Password     | Role  | Access |
| -------- | ------------ | ----- | ------ |
| `admin`  | `Admin@123`  | ADMIN | Employees (full CRUD) and Reports |
| `viewer` | `Viewer@123` | USER  | Employees (read-only); the Reports menu is hidden and its API returns 403 |

What happens:

1. The `Dockerfile` builds the Angular app, copies it into the Spring Boot jar, runs the backend tests (on in-memory H2), and produces a small JRE image that runs as a non-root user.
2. `docker-compose.yml` starts MySQL 8.4 (data kept in the `mysql-data` volume), waits until it is healthy, then starts the app.
3. On first start, Flyway creates the schema (`V1`) and loads demo data (`V2`: 6 departments, 25 fictional employees, 2 users).

Useful commands:

```sh
docker compose logs -f app                        # application logs
docker compose down                               # stop (data kept)
docker compose down -v                            # stop and delete the database volume
docker compose build --build-arg SKIP_TESTS=true  # faster image build
```

> If `JWT_SECRET` is empty, the app generates a random signing key at each start, so everyone is signed out after a restart. Set a value of at least 32 characters for stable sessions (`openssl rand -base64 48`).

## Local development (without Docker for the app)

Prerequisites: Java 21, Maven 3.9+, Node.js 22, and a MySQL 8 server. The easiest way to get MySQL is `docker compose up -d mysql`.

```sh
# Backend → http://localhost:8080
cd backend
mvn spring-boot:run       # uses DB_URL / DB_USERNAME / DB_PASSWORD, defaulting to localhost:3306 peopledesk/peopledesk

# Frontend → http://localhost:4200  (proxy.conf.json forwards /api to :8080)
cd frontend
npm ci
npm start
```

Run the backend tests: `cd backend && mvn test` (no MySQL needed; uses H2 in MySQL mode with the real Flyway schema).

### Configuration

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `DB_URL` | `jdbc:mysql://localhost:3306/peopledesk?...` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `peopledesk` / `peopledesk` | Database credentials |
| `JWT_SECRET` | *(random per start)* | HMAC key, ≥ 32 bytes |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200,http://127.0.0.1:4200` | Only needed if the SPA calls the API cross-origin |
| `PORT` | `8080` | HTTP port |

Token lifetimes and the export row limit are in `backend/src/main/resources/application.yml`.

## Features and use cases

| Use case | Implementation |
| -------- | -------------- |
| UC-LOGIN-01 Login | `POST /api/auth/login`. JWT + refresh token stored in localStorage, redirect to `/employees`, generic error message for bad credentials |
| UC-LOGIN-02 Validation | Reactive form: username required, ≥ 3 chars; password required, ≥ 6 chars; submit disabled until valid. Same rules on the server |
| UC-LOGIN-03 Remember me | Remembers the username and extends the refresh-token lifetime from 8 h to 7 days. Access tokens are refreshed automatically (`/api/auth/refresh`, single-use rotating tokens) |
| UC-LOGIN-04 Session timeout | 30-minute idle timer (mouse, keyboard, scroll) with a one-minute warning and a "Stay signed in" action |
| UC-TXN-01 View | Material table with server-side pagination (10 per page), sorting on every column, and filters |
| UC-TXN-02 Add | Dialog form with validation, success toast, table and summary cards refresh |
| UC-TXN-03 Edit | Click a row (or Edit) to open a pre-filled dialog that sends `PUT` |
| UC-TXN-04 Delete | Confirmation dialog, then a hard `DELETE` with a toast; the table refreshes |
| UC-TXN-05 Validation | Client: required, length, custom `workEmail` / `phoneNumber` / `notBlank` / `notInFuture` validators. Server: Bean Validation and a case-insensitive duplicate-email check (409 shown on the email field), with a DB `UNIQUE` constraint as the last line of defence |
| UC-TXN-06 Search | Real-time search on name, email, and title with a 300 ms debounce; department and status filters; clear buttons |
| UC-RPT-01 View report | Read-only paginated, sortable table with date-range, department, and status filters |
| UC-RPT-02 Excel | Server-side Apache POI `.xlsx` with every filtered row, styled headers, frozen header row, auto-filter, and a "Report info" sheet |
| UC-RPT-03 PDF | Server-side OpenPDF: landscape, title, filters and generation date, header row repeated on each page, company header/footer, page numbers |
| UC-RPT-04 Summary | Cards (total and per-status counts with percentages) plus department and join-month breakdowns from SQL `COUNT … GROUP BY` |
| UC-RPT-05 Date range | Material date-range picker, defaulting to the 1st of this month through today; from ≤ to is validated on client and server |
| UC-LOGOUT-01 Logout | Clears tokens from localStorage, redirects to `/login`; the backend deletes the refresh token and revokes the access token's `jti` |
| UC-LOGOUT-02 Route guard | `authGuard` on every route except `/login`; `adminGuard` on `/reports`; expired sessions redirect to login |
| Role-based menu | `ADMIN` sees Reports and the edit actions; `USER` sees a read-only directory. The backend enforces the same rules |

## API reference

All endpoints are under `/api` and use JSON. Except for login, refresh, and logout, every request needs `Authorization: Bearer <accessToken>`.

| Method | Path | Role | Description |
| ------ | ---- | ---- | ----------- |
| POST | `/auth/login` | public | `{username, password, rememberMe}` → `{accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt, user}` |
| POST | `/auth/refresh` | public | `{refreshToken}` → new token pair (the old refresh token becomes invalid) |
| POST | `/auth/logout` | public | `{refreshToken}`; also revokes the bearer token if one is sent → 204 |
| GET | `/auth/me` | any | Current user `{username, fullName, role}` |
| GET | `/departments` | any | `[{id, name}]` |
| GET | `/employees` | any | Query: `page` (0-based), `size` (≤ 100, default 10), `sort` (`name\|email\|title\|department\|status\|joined`), `direction` (`asc\|desc`), `search`, `departmentId`, `status` (`ACTIVE\|ON_LEAVE\|INACTIVE`) → `{content, page, size, totalElements, totalPages}` |
| GET | `/employees/stats` | any | `{total, active, onLeave, inactive, departments}` |
| GET | `/employees/{id}` | any | One employee |
| POST | `/employees` | ADMIN | Create → 201 + `Location` header |
| PUT | `/employees/{id}` | ADMIN | Update |
| DELETE | `/employees/{id}` | ADMIN | Delete → 204 |
| GET | `/reports/employees` | ADMIN | Query: `from`, `to` (yyyy-MM-dd, default: current month), `departmentId`, `status`, plus paging/sorting as above |
| GET | `/reports/summary` | ADMIN | Same filters → `{from, to, total, byStatus[], byDepartment[], byJoinMonth[]}` |
| GET | `/reports/export/excel` | ADMIN | Same filters + `sort`/`direction` → `.xlsx` download |
| GET | `/reports/export/pdf` | ADMIN | Same filters + `sort`/`direction` → `.pdf` download |
| GET | `/actuator/health` | public | Health check (used by Docker) |

Employee request body:

```json
{ "name": "Ada Lovelace", "email": "ada@example.com", "phone": "+1 (202) 555-0199",
  "title": "Engineer", "departmentId": 1, "status": "ACTIVE", "joinedOn": "2026-01-05" }
```

Every error uses the same body shape:

```json
{ "timestamp": "…", "status": 409, "error": "Conflict", "message": "An employee with this email already exists.",
  "path": "/api/employees", "fieldErrors": { "email": "An employee with this email already exists." } }
```

Status codes: 400 validation or bad parameters (`fieldErrors` per field) · 401 missing, invalid, expired, or revoked token · 403 wrong role · 404 not found · 409 duplicate.

## Database design

```
departments (id PK, name UNIQUE NOT NULL)
     ▲ 1
     │ N   fk_employees_department
employees (id PK, name, email UNIQUE, phone, title, department_id FK NOT NULL,
           status CHECK IN ('ACTIVE','ON_LEAVE','INACTIVE'), joined_on DATE,
           created_at, updated_at)            indexes: joined_on, status, name

users (id PK, username UNIQUE, password_hash (BCrypt), full_name, role CHECK IN ('ADMIN','USER'), created_at)
     ▲ 1
     │ N   fk_refresh_tokens_user (ON DELETE CASCADE)
refresh_tokens (id PK, token_hash UNIQUE (SHA-256), user_id FK, expires_at, created_at)

revoked_access_tokens (jti PK, expires_at)    -- access tokens revoked at logout
```

- Migrations: `backend/src/main/resources/db/migration` (schema) and `db/seed` (demo data, MySQL only).
- Hibernate runs with `ddl-auto: validate`, so the application refuses to start if entities and schema drift apart.
- Employee emails are trimmed and lower-cased before saving, so uniqueness is case-insensitive in any collation.
- An hourly job deletes expired refresh tokens and revocation entries.

## Security

- **Passwords**: stored only as BCrypt hashes; the seed file contains hashes, not passwords.
- **JWT**: HS256 with a server secret; issuer and expiry are verified. Access tokens last 15 minutes. Refresh tokens are 256-bit random values stored as SHA-256 hashes, single-use, with an absolute expiry.
- **Authorization**: enforced in `SecurityConfig` by URL and HTTP method, independent of the Angular guards.
- **SQL injection**: all queries are Spring Data / JPQL / Criteria API with bound parameters. LIKE wildcards in search text are escaped. Sort fields are whitelisted.
- **Input validation**: Bean Validation on every request body, mirrored by the Angular validators.
- **Headers**: Content-Security-Policy (`script-src 'self'`, `frame-ancestors 'none'`), `X-Content-Type-Options`, `X-Frame-Options: DENY`. CSRF protection is off because the API is stateless and uses bearer headers, not cookies.
- **Errors**: stack traces are never returned; unexpected errors are logged on the server and the client gets a generic message.
- **Container**: runs as a non-root user; secrets come from environment variables.
- **Known trade-offs**: storing the JWT in localStorage was required by the assessment, but an XSS bug could read it (the CSP reduces that risk; an HttpOnly cookie would avoid it). There is no login rate limiting or account lockout yet.

## Testing

```sh
cd backend && mvn test
```

34 tests:

- `JwtServiceTest`: token round trip; rejects expired, tampered, foreign-key, and garbage tokens; rejects short secrets.
- `AuthServiceTest`: login, remember-me lifetime, generic bad-credential message, refresh rotation and expiry, logout revocation.
- `EmployeeServiceTest`: email normalization, duplicate and unknown-department rejection, update/delete, stats, sort whitelist, LIKE escaping.
- `ReportServiceTest`: default current-month range, rejects from > to.
- `ApiIntegrationTest`: full HTTP stack on H2 with the real Flyway schema. Covers 401/403 rules, CRUD, validation errors, 409 duplicates, SQL-injection-style search, the viewer role, report summary/rows, and Excel/PDF downloads.

The frontend has no automated tests. It was verified with a scripted headless-Chrome run (see `AI_USAGE_LOG.md`).

## Project structure

```
backend/
  src/main/java/com/peopledesk/
    auth/         login, refresh, logout, refresh-token + revocation entities, cleanup job
    security/     SecurityConfig, JwtService, JwtAuthenticationFilter, UserDetailsService
    employee/     entity, repository (+ aggregate queries), Criteria specifications, service, controller, DTOs
    department/   entity, repository, controller
    report/       report service/controller, Excel and PDF writers, summary DTOs
    user/         User entity, Role, repository
    common/       error model, exception handler, page DTO
    config/       clock bean, SPA deep-link forwarding
  src/main/resources/db/{migration,seed}/   Flyway SQL
frontend/src/app/
  core/       API models, AuthService, interceptor, guards, idle timeout, HTTP helpers
  employees/  directory page, add/edit dialog, delete dialog, validators, service
  reports/    report page and service
  shared/     server-driven employee table
  layout/     sidebar shell
  login/      login page
Dockerfile, docker-compose.yml, .env.example
```
