# Maintenance App — Master Development Roadmap

## 1. Purpose and working rules

This file is the single source of truth for phase order and current project progress. Every coding agent working on this project must read this file and `DEVELOPMENT_TRACKER.md` before changing code.

### Project overview
- Backend: Java + Spring Boot, REST APIs, Spring Security/JWT, database persistence.
- Frontend: React Native + Expo.
- API documentation/testing: Swagger/OpenAPI and Postman where applicable.
- Product: residency maintenance management for residents and admins.
- Existing business rules include monthly maintenance, payment proof submission, admin verification, cash requests, payment allocations, and room exemption rules. Read the existing code/configuration before relying on assumptions.

### Mandatory agent rules
1. **Identify the active phase first.** Read `PROJECT_ROADMAP.md` and `DEVELOPMENT_TRACKER.md`. State the active phase and the exact task before making changes.
2. Work only on the active phase and its agreed task. Do not start later phases unless explicitly instructed.
3. Audit existing code before implementing. Reuse working APIs, DTOs, services, repositories, components, navigation, and styling. Do not duplicate or rewrite functioning features without a clear reason.
4. Do not assume class names, routes, DTO fields, enum values, authentication claims, token response shapes, or storage solutions. Inspect the code.
5. Backend and frontend work should be completed together for each feature whenever practical: API gap analysis/implementation → API tests → UI design/implementation → integration tests.
6. Do not weaken authentication, authorization, validation, or financial business rules to make a feature work.
7. Follow the existing architecture and naming conventions. Prefer concrete `@Service` classes; do not introduce service interfaces/implementation pairs without a genuine need. Do not use Lombok `@Builder` or builder-based construction unless the existing project explicitly requires it.
8. Never hardcode secrets, JWTs, credentials, local IP addresses, or production URLs in source code. Use existing environment/configuration patterns.
9. Do not add dependencies or upgrade package versions unless necessary. Explain the reason first in the completion report.
10. Run relevant tests/checks. Never claim something is tested or complete unless the command was actually run and its result is known.
11. Preserve unrelated changes and avoid destructive Git commands.
12. At the end of each task, update `DEVELOPMENT_TRACKER.md` and the phase status below. Record changed files, commands/tests and their real results, known issues, and next step.
13. If a requirement is unclear or the existing design conflicts with it, report the conflict instead of silently inventing a rule.
14. Do not mark a phase complete while required tests, integration, or user-visible acceptance checks remain unfinished.

## 2. Phase status board

Update this board whenever progress changes. Use `NOT STARTED`, `IN PROGRESS`, `BLOCKED`, or `COMPLETE`. A phase is complete only after its acceptance criteria pass.

- [x] Phase 0 — Existing implementation audit and project baseline — Status: COMPLETE
- [ ] Phase 1 — Authentication, session handling, and app foundation — Status: IN PROGRESS
- [ ] Phase 2 — Resident dashboard and maintenance history — Status: NOT STARTED
- [ ] Phase 3 — Online payments and proof submission — Status: NOT STARTED
- [ ] Phase 4 — Cash payment request workflow — Status: NOT STARTED
- [ ] Phase 5 — Admin dashboard and financial overview — Status: IN PROGRESS (Backend Complete; Frontend Pending)
- [ ] Phase 6 — Admin payment review and allocation management — Status: NOT STARTED
- [ ] Phase 7 — Resident, room, and account administration — Status: NOT STARTED
- [ ] Phase 8 — Cross-feature QA, deployment, and Play Store release — Status: NOT STARTED

**Current active phase:** Phase 1 — Authentication, session handling, and app foundation  
**Current active task:** Verify login, registration, and role-based redirect in frontend against backend auth contracts with physical/device checks.  
**Do not infer completion from this roadmap alone.** Verify the implementation and tests.

## 3. Phase definitions and acceptance criteria

### Phase 0 — Existing implementation audit and project baseline
**Goal:** Establish an accurate baseline so agents do not rebuild existing work.

Tasks:
- Inspect backend and frontend project structure.
- Review current `DEVELOPMENT_TRACKER.md`, API routes/Swagger, entities, DTOs, services, repositories, security, config, and tests.
- Review frontend screens, navigation, API client, environment config, token storage, shared components, and existing styles.
- Confirm the actual response/request contracts for login, registration, dashboard, maintenance, payments, cash requests, and image upload.
- Verify the status of `GET /api/users/me/dashboard` and any admin dashboard endpoint against actual code.
- Record missing items, broken items, and unverified items separately.

Acceptance:
- Tracker reflects verified reality, not assumptions.
- API inventory and frontend screen inventory are recorded.
- No feature is labelled complete solely because a route appears in Swagger.

### Phase 1 — Authentication, session handling, and app foundation
**Goal:** Make sign-in and registration reliable and establish navigation/API infrastructure.

Tasks:
- Finish or verify login and registration screens and validation.
- Confirm request/response contracts against backend.
- Configure one reusable Axios client and environment-based API URL.
- Attach JWT through the existing interceptor/storage approach.
- Restore session on app launch according to actual token expiry/refresh design.
- Implement protected navigation and route users based on the authenticated role; never ask the user to select a role at login.
- Handle logout, invalid credentials, expired/invalid tokens, network errors, loading states, and unauthorized responses.
- Ensure no secrets or tokens are logged.

Acceptance:
- Registration and login work on a physical device.
- Correct role-based navigation works.
- Relaunch behavior is correct and consistent with token policy.
- Logout clears session correctly.
- Unauthorized and network-error cases are handled.
- Tests/checks pass and tracker is updated.

### Phase 2 — Resident dashboard and maintenance history
**Goal:** Give residents an accurate view of their room, maintenance, and balance.

Tasks:
- Verify and integrate `GET /api/users/me/dashboard`.
- Display resident name, room number, maintenance rate, outstanding balance, current maintenance, and recent payment according to the actual DTO.
- Implement maintenance history using existing `GET /api/maintenance/my` and other suitable existing endpoints.
- Show clear states for `PENDING`, `PARTIALLY_PAID`, and `PAID` (using actual enum values).
- Add loading, empty, error, and pull-to-refresh states where useful.
- Do not calculate financial balances independently in the UI; use backend-calculated values.
- Ensure exempt rooms and partial allocations are represented correctly.

Acceptance:
- Dashboard uses live API data, not mock data.
- Monthly history matches backend records.
- Balance/status values match verified backend calculations.
- Loading, empty, and error states work.

### Phase 3 — Online payments and proof submission
**Goal:** Allow residents to submit online payment proof for admin verification.

Tasks:
- Inspect current payment lifecycle and endpoints, including create payment, submit/update proof, get own payments, and image upload.
- Display the configured admin payment QR/details using the existing design/API or identify a missing requirement.
- Implement create-payment and UTR/screenshot proof submission in the correct order.
- Integrate image upload only after confirming its actual API contract and storage behavior.
- Show payment history and statuses; prevent duplicate submissions and unsafe retries.
- Enforce authenticated room ownership on the backend.
- Do not mark payments paid merely because a resident submitted proof.

Acceptance:
- Resident can create a payment and submit proof.
- Uploaded proof can be reviewed by admin.
- Pending payment is not counted as verified collection.
- Invalid uploads, duplicate actions, and network failures are handled.
- Existing payment tests continue passing.

### Phase 4 — Cash payment request workflow
**Goal:** Support residents requesting verification of cash payments.

Tasks:
- Integrate cash request creation and resident request history using existing routes.
- Display amount, status, admin note, created time, and verification/rejection information where provided.
- Prevent invalid amounts and duplicate/repeated submissions according to existing rules.
- Ensure only an admin can verify/reject requests.
- Verify that acceptance creates/records the appropriate cash payment and allocations exactly once.
- Confirm rejected/pending requests do not reduce outstanding balances.

Acceptance:
- Resident can create and track a cash request.
- Admin verification/rejection is reflected correctly.
- Allocation and balance changes are correct and not duplicated.
- Ownership/role checks and tests pass.

### Phase 5 — Admin dashboard and financial overview
**Goal:** Give admins a reliable property-wide overview.

Tasks:
- Inspect whether `GET /api/admin/dashboard` exists and is correct; implement it only if missing or incomplete.
- Display room count, collected/verified amounts, outstanding dues, pending online payments, and pending cash requests according to agreed metric definitions and actual DTO.
- Reuse backend business logic for allocations and balances.
- Add navigation from summary cards to the relevant management screen.
- Show loading, empty, and error states.

Acceptance:
- Admin-only access is enforced.
- Summary numbers match database-backed records and do not double-count.
- Residents cannot access admin metrics.
- UI is connected to real APIs.

### Phase 6 — Admin payment review and allocation management
**Goal:** Complete the admin workflow for reviewing and processing payments.

Tasks:
- Build pending online-payment list and payment detail/proof view.
- Implement verify/reject actions using current endpoints and actual request contracts.
- Build pending cash request list and detail view; wire verify/reject and admin notes.
- Show payment allocation history and affected maintenance months.
- Add useful filters/search/pagination only where needed and supported.
- Add confirmation dialogs for consequential financial actions and prevent repeated taps/submissions.
- Check authorization and server-side validation for all admin actions.
- Add missing backend endpoints only after confirming existing APIs are insufficient.

Acceptance:
- Admin can review proof and verify/reject a payment.
- Cash request decisions are processed correctly.
- Maintenance allocations are correct, auditable, and not duplicated.
- Loading/success/error states and authorization checks work.

### Phase 7 — Resident, room, and account administration
**Goal:** Complete account and room administration needed for normal operation.

Tasks:
- Inspect existing profile/user/room endpoints before adding any.
- Build profile/account details and password change/reset UI using existing APIs.
- Build admin room/resident overview if required by the current product scope.
- Support room exemption/admin designation changes only if authorized backend support exists or is deliberately implemented.
- Ensure the current admin belongs to a room if that is an established domain requirement.
- Prevent unauthorized user lookup or room manipulation.
- Do not add complaint management unless explicitly brought back into scope.

Acceptance:
- Required account-management flows work end to end.
- Admin-only operations are protected.
- Room/admin/exemption rules match backend configuration and tests.
- No duplicate endpoints or conflicting business rules.

### Phase 8 — Cross-feature QA, deployment, and Play Store release
**Goal:** Prepare the complete application for release.

Tasks:
- Run backend unit/integration tests and frontend lint/type/build checks available in the project.
- Test resident and admin workflows from login through payments and verification.
- Test partial payments, unpaid months, exempt rooms, duplicate requests, failed uploads, expired tokens, and unauthorized access.
- Verify API base URLs, CORS, secrets, database migrations, logging, and production configuration.
- Check responsive/mobile layouts, keyboard behavior, accessibility, and Android device compatibility.
- Configure production hosting and environment variables.
- Create the Expo production build and prepare app icon, splash screen, package/application ID, versioning, privacy policy, and Play Console submission requirements.
- Document deployment and recovery steps.

Acceptance:
- All critical flows pass on a physical device/build.
- No known critical authorization or financial correctness issue remains.
- Production configuration contains no development secrets or localhost API URLs.
- Build and release checklist are complete.

## 4. Definition of done for each feature
A feature is done only when all applicable items below are complete:
- [ ] Existing code audited and implementation plan stated.
- [ ] Backend API/business logic complete or existing API verified.
- [ ] Request/response validation and authorization checked.
- [ ] Swagger/Postman/API tests passed where applicable.
- [ ] Frontend design and screen implementation complete.
- [ ] Real API integration complete; temporary mock data removed unless explicitly required for a demo.
- [ ] Loading, empty, success, and error states handled.
- [ ] Critical edge cases tested.
- [ ] Existing tests/checks run and results recorded.
- [ ] `DEVELOPMENT_TRACKER.md` updated.
- [ ] Phase status updated in this file.
- [ ] Remaining issues and next task documented.

## 5. Required agent response format
At the start of every task, report:
1. Roadmap current active phase.
2. Requested task and whether it belongs to that phase.
3. Existing code/features found that can be reused.
4. Missing work and implementation plan.

At the end, report:
1. What changed and files changed.
2. APIs/screens implemented or reused.
3. Commands/tests run and actual outcomes.
4. Manual testing still required.
5. Known issues/blockers.
6. Tracker and phase status updates.
7. The recommended next task, without starting it automatically.

## 6. Phase transition protocol
- Only the project owner can authorize moving to a new phase.
- When the active phase meets its acceptance criteria, mark it `COMPLETE`, summarize evidence, and recommend the next phase.
- Do not change the active phase just because code was written; require test and integration evidence.
- If a phase is blocked, mark it `BLOCKED`, explain why, and propose the smallest unblock step.
- Keep this roadmap concise and current; do not create a separate competing roadmap.

## 7. Known project context — verify before relying on it
- Frontend is React Native + Expo.
- Backend is Spring Boot with JWT-based authentication.
- Login and registration APIs were reported as implemented.
- Swagger previously showed endpoints for admin password reset, user lookup, maintenance, payment allocations, authentication, rooms, image uploads, online payments, and cash payment requests.
- `GET /api/maintenance/my` and `GET /api/payments/my` appeared in Swagger.
- `GET /api/users/me/dashboard` is verified and tested (13 tests passing).
- `GET /api/admin/dashboard` backend endpoint is fully implemented, verified, and tested (14 tests passing including live PostgreSQL integration); frontend UI integration remains pending in Phase 5.
- Monthly maintenance was previously specified as ₹500, with exemption and partial-allocation rules. Verify current configuration and domain logic before changing anything.
- Payment flow is create payment → submit proof → admin verification. A submitted proof alone must not mean verified payment.
- Do not assume image upload integration is complete: inspect its endpoint, storage service, and frontend integration.
- No complaint feature is currently in scope unless the project owner explicitly reintroduces it.
- Use actual current repository contents as authoritative when they differ from this context.
