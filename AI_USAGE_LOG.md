# AI usage log

## Frontend implementation — November 6, 2026

**Tool:** OpenAI Codex.

**User request:** Start the downloaded Application Developer assessment from scratch, beginning with the frontend.

**Generated:** Angular 19 project configuration, standalone routed components, Material interface, reactive employee/login forms, demo session and idle timeout, employee browser storage, sorting/pagination/filtering, Excel/PDF export, and setup documentation.

**Review and modifications:** Kept authentication explicitly in demo mode rather than creating a fake JWT. Restricted the future bearer-token interceptor to the backend API origin and path. Added case-insensitive duplicate email checks, trim handling, validation, delete confirmation, invalid report date handling, browser storage failure messages, and lazy-loaded export libraries. Escaped the @ character in Angular template text and moved the theme declaration to eliminate a Sass warning during compilation.

**Developer learning to complete personally:** Run the app, trace one employee from its form to the service and table, explain reactive validators and route guards, explain why frontend checks cannot replace backend security, and describe how filtered rows feed the export functions. Add your own notes here after reviewing and modifying the code. This log does not claim you have already reviewed or learned these topics.

**Validation:** Strict TypeScript and Angular template checks passed, followed by a successful production build with Angular disk caching disabled. Tested the primary browser flows and both report downloads; reopened the exported workbook to check headers and filtered row count. See README for exact coverage and verification limits.

## Backend, database, frontend integration and Docker — November 8, 2026

**Tool:** Claude Code (Claude Opus 5.5), run in agent mode against this repository.

**Prompt given:** "Based on the given assessment, build the application given in the assessment with all the frontend, backend with databases and finally build a docker file to run the application in a docker container. […] There is some code which is already built. Check if that works and modify accordingly."

**Review of the existing code:** The Angular frontend built successfully but was demo-only. Login was a hard-coded credential check, data lived in localStorage, and filtering, sorting, paging, and exports ran in the browser. There was no backend, database, or Docker setup.

**Generated:**
- A Spring Boot 3.4 / Java 21 backend: JWT authentication with rotating refresh tokens and logout revocation, BCrypt users, ADMIN/USER roles, employee CRUD with server-side paging, sorting, and filtering (Criteria API), report endpoints with SQL `GROUP BY` aggregates, and Apache POI / OpenPDF exports.
- A MySQL schema via Flyway (PK, FK, UNIQUE, NOT NULL, CHECK constraints, indexes) and seed data.
- 34 JUnit tests (unit tests with Mockito, plus a full-stack MockMvc test on H2).
- A refactor of the frontend to call the API: interceptor with transparent token refresh, role-aware guards and menu, server-driven Material table, date pickers, server error mapping onto form fields, and server-side exports. The client-side export libraries (exceljs, jspdf) were removed.
- A multi-stage `Dockerfile`, `docker-compose.yml` with MySQL, and documentation.

**Problems found while reviewing and running the generated code, and the fixes:**
- Hibernate 6 maps `@Enumerated(STRING)` to a native MySQL `ENUM`, which would fail `ddl-auto: validate` against the portable `VARCHAR + CHECK` columns. Added `@JdbcTypeCode(SqlTypes.VARCHAR)`.
- The default LIKE escape character `\` is itself an escape in MySQL string literals. Switched to `!` and added a test showing that `%`, `_`, and SQL fragments in search text match nothing.
- An expired refresh token was deleted and then an exception was thrown, which rolled back the delete. Added `noRollbackFor`.
- An unknown department ID was first mapped to 409 Conflict; changed it to a 400 field error.
- Angular's production "inline critical CSS" uses an inline `onload` handler that the new Content-Security-Policy blocks, so styles would stay disabled. Turned `inlineCritical` off.
- A strict-template type error on the status-label lookup; fixed with a typed helper method.
- Validation messages in the add/edit dialog wrapped into the field below (seen in a screenshot). Shortened the messages and increased the row gap.
- A missing favicon caused a 404 console error. Added an inline SVG icon.

**How the output was verified:** `mvn test` (34/34 passing) inside a Maven container. `docker compose up --build` against MySQL 8.4 (Flyway migrations applied, schema validated). curl checks of every endpoint and role. A scripted headless-Chrome walkthrough (36 checks): guard redirect, login validation, wrong password, remember me, server paging and sorting, 300 ms debounced search, add/duplicate/edit/delete with toasts, report date-range validation, Excel and PDF downloads, logout, viewer role, mobile layout with no horizontal scroll, transparent token refresh, and no CSP violations. The exported PDF was rendered and inspected visually.

**Developer learning (to complete personally):** Trace a request from `employees.component.ts` → `auth.interceptor.ts` → `JwtAuthenticationFilter` → `EmployeeController` → `EmployeeSpecifications` → SQL. Be able to explain why refresh tokens are hashed and rotated, why the backend repeats every frontend check, and how `GROUP BY` results become the report cards. Add your own notes and any changes you make here.
