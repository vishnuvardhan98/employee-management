# PeopleDesk — Employee Management

An Angular 19 + Angular Material frontend for the Application Developer assessment. This phase runs independently with browser demo data. Spring Boot and MySQL are the next phase.

## Run locally

Install a current Node.js 22 release and npm (verified with Node 22.23.4). Open a terminal:

```sh
cd frontend
npm ci
npm start
```

Open http://127.0.0.1:4200. Sign in with **admin / Admin@123**.

```sh
npm run build
```

Production files are generated in `frontend/dist/employee-management/browser`. Any static hosting must fall back to index.html for Angular routes.

## What you can try

1. Open /employees while signed out: you should be redirected to login.
2. Login fields require a username of at least 3 characters and a password of at least 6. Incorrect demo credentials show an error. Remember me stores only the username.
3. Search by name, email, or job title. Search waits 300 ms before filtering. Use department/status filters, column sorting, and the page controls (10 records by default).
4. Add an employee, edit by selecting a row or Edit, and delete through the confirmation dialog. Email addresses must be unique, ignoring case. Changes survive refresh in the same browser.
5. Open Reports. The date range defaults to this month and filters the employee join date. Try department/status filters and an invalid date range.
6. Export Excel or PDF: every filtered record is exported, including records on other pages. Empty/invalid reports disable export.
7. Sign out: protected routes become inaccessible. A 30-minute idle timer warns at 29 minutes before signing out. The demo session also expires after 8 hours.

## Source guide

- `frontend/src/main.ts`: starts Angular and registers routing, animations, and HTTP support.
- `frontend/src/app/app.routes.ts`: public login and protected employee/report routes.
- `frontend/src/app/auth.service.ts`: demo session, route guard, idle timeout, and future API token interceptor.
- `frontend/src/app/login.component.ts`: reactive login form.
- `frontend/src/app/shell.component.ts`: sidebar, workspace header, navigation, sign-out.
- `frontend/src/app/employee.service.ts`: typed employee model, fictional sample data, and browser persistence.
- `frontend/src/app/employees.component.ts`: directory, summary cards, search/filter controls, and dialog actions.
- `frontend/src/app/employee-dialog.component.ts`: reactive add/edit form and delete confirmation.
- `frontend/src/app/employee-table.component.ts`: shared Material table, sorting, pagination, and empty state.
- `frontend/src/app/reports.component.ts`: report filters, summaries, and lazy-loaded ExcelJS/jsPDF exports.
- `frontend/src/styles.scss`: Material theme, application layout, mobile layout.

## Current boundaries

This is a frontend demonstration, not secure authentication. The demo credentials and session are intentionally visible client-side. It does not issue fake JWTs. Do not store real employee information in this demo.

Sorting, filtering, pagination, validation, and summaries currently happen in the browser. Backend enforcement, real JWT login, BCrypt password storage, roles, database uniqueness constraints, SQL aggregation, and server pagination remain for Spring Boot/MySQL integration. There is no optional token refresh implementation yet. Browser localStorage can be cleared to reset demo data.

The interceptor is prepared to attach `peopledesk.jwt` only to http://localhost:8080/api/; demo login does not populate this key. Remove demo sessions when integrating real login. Backend authorization must enforce all protected operations regardless of the frontend guard.

## Planned API contract (not implemented yet)

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | /api/auth/login | Validate credentials; return real access token, expiry, and user roles |
| GET | /api/employees?page=0&size=10&sort=name,asc&search= | Server search, filters, sorting, pagination |
| POST | /api/employees | Validate and create employee |
| PUT | /api/employees/{id} | Validate and update employee |
| DELETE | /api/employees/{id} | Delete employee |
| GET | /api/reports/employees?from=YYYY-MM-DD&to=YYYY-MM-DD | Filtered report rows |
| GET | /api/reports/summary?from=YYYY-MM-DD&to=YYYY-MM-DD | Database summary/aggregations |

Use Employee DTO fields: id, name, email, phone, department, title, status, joined. Standardize status enum mapping and error responses with the backend. Return validation errors with field messages; map duplicate email to a user-friendly conflict response. Add a Spring Boot development CORS rule for http://127.0.0.1:4200 or use an Angular development proxy.

## Learning path

Start with main.ts → routes → login component → auth service. Then read the Employee interface and service before following the directory component and dialog. Finally trace filtered report data into the export functions. Angular standalone components import the Material controls and form features they use directly; Reactive Forms keep input values and validation rules in TypeScript.

Official Angular references: https://v19.angular.dev/guide/forms and https://angular.dev/api/platform-browser/bootstrapApplication.

## Verification performed

- Strict TypeScript check and Angular template compilation passed.
- Angular production build passed. Export libraries emit CommonJS optimization warnings; they are loaded only when an export is requested.
- Browser checks passed for disabled invalid login, wrong credentials, successful login, employee creation/editing, 300 ms search, pagination, delete confirmation/cancel, report invalid date ranges, export downloads, sign-out, and protected-route redirection.
- The downloaded Excel workbook was reopened: eight columns and seven filtered employee rows matched the report. PDF download completed; its visual contents have not been independently inspected.
- Refresh retained the browser test employee and its updated title. One fictional Demo Tester record was added during testing, in browser storage only. A fresh browser starts with 18 seed records.
- The 30-minute idle timeout has not been tested with a full elapsed-time wait. Delete confirmation was tested with Cancel; permanent removal was not exercised in the browser.
- Angular disk caching is disabled because the native cache crashed in this environment. If macOS reports too many file watchers, use `npm start -- --poll 2000 --hmr=false`.

There is no automated frontend test suite in this initial version; the above are build and manual browser checks.
