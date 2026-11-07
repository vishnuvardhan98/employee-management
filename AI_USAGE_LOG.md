# AI usage log

## Frontend implementation — November 6, 2026

**Tool:** OpenAI Codex.

**User request:** Start the downloaded Application Developer assessment from scratch, beginning with the frontend.

**Generated:** Angular 19 project configuration, standalone routed components, Material interface, reactive employee/login forms, demo session and idle timeout, employee browser storage, sorting/pagination/filtering, Excel/PDF export, and setup documentation.

**Review and modifications:** Kept authentication explicitly in demo mode rather than creating a fake JWT. Restricted the future bearer-token interceptor to the backend API origin and path. Added case-insensitive duplicate email checks, trim handling, validation, delete confirmation, invalid report date handling, browser storage failure messages, and lazy-loaded export libraries. Escaped the @ character in Angular template text and moved the theme declaration to eliminate a Sass warning during compilation.

**Developer learning to complete personally:** Run the app, trace one employee from its form to the service and table, explain reactive validators and route guards, explain why frontend checks cannot replace backend security, and describe how filtered rows feed the export functions. Add your own notes here after reviewing and modifying the code. This log does not claim you have already reviewed or learned these topics.

**Validation:** Strict TypeScript and Angular template checks passed, followed by a successful production build with Angular disk caching disabled. Tested the primary browser flows and both report downloads; reopened the exported workbook to check headers and filtered row count. See README for exact coverage and verification limits.
