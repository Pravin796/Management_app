# Maintenance App — Development Tracker

Last Updated: 2026-10-24  
Single source of truth for verified progress, test evidence, blockers, and active tasks.

---

## 1. Executive Summary & Current Phase Status

| Phase | Title | Status | Verified Evidence |
| :--- | :--- | :---: | :--- |
| **Phase 0** | Existing implementation audit and project baseline | **COMPLETE** | Full backend/frontend audit conducted; API & screen inventory completed; all initial baseline tests pass. |
| **Phase 1** | Authentication, session handling, and app foundation | **IN PROGRESS** | Backend auth APIs verified (`/api/auth/register`, `/api/auth/login`, `/api/auth/change-password`). Frontend navigation & session restoration in place in `App.js`. Physical device acceptance test pending. |
| **Phase 2** | Resident dashboard and maintenance history | **IN PROGRESS** | Backend `GET /api/users/me/dashboard` verified & passing 13 tests. Frontend `DashboardScreen.js` UI complete; live API service hookup pending. |
| **Phase 3** | Online payments and proof submission | **IN PROGRESS** | Backend payments + Cloudinary proof upload APIs verified. Frontend screens `PaymentScreen.js` & `OnlinePaymentScreen.js` exist. |
| **Phase 4** | Cash payment request workflow | **IN PROGRESS** | Backend cash request & admin verify/reject APIs verified with dedicated unit tests. Frontend `CashPaymentScreen.js` exists. |
| **Phase 5** | Admin dashboard and financial overview | **IN PROGRESS** | Backend `GET /api/admin/dashboard` fully implemented and verified (14 unit/controller/integration tests). Frontend `AdminDashboardScreen.js` UI exists but currently uses local mock state. |
| **Phase 6** | Admin payment review and allocation management | **NOT STARTED** | Backend allocation & verify APIs exist (`/api/payment-allocations`, `/api/payments/*/verify`, `/api/cash-payments/*/verify`). Frontend wiring pending. |
| **Phase 7** | Resident, room, and account administration | **NOT STARTED** | Backend user & room CRUD exists. Admin UI screens pending. |
| **Phase 8** | Cross-feature QA, deployment, and Play Store release | **NOT STARTED** | Release prep pending. |

---

## 2. Verified API Inventory (Backend)

| Method | Endpoint | Auth / Roles | Status | Verified Tests |
| :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/auth/register` | Public | Active | Integration / Unit |
| `POST` | `/api/auth/login` | Public | Active | Integration / Unit |
| `POST` | `/api/auth/change-password` | Authenticated | Active | Unit |
| `GET` | `/api/users/me/dashboard` | `ROLE_USER`, `ROLE_ADMIN` | **Active & Verified** | `UserControllerTest` (2 tests), `UserServiceDashboardTest` (11 tests) |
| `GET` | `/api/users/{id}` | Admin or Self | **Active & Verified** | `UserControllerTest` (4 tests), `UserServiceAuthorizationTest` (4 tests) |
| `POST` | `/api/admin/users/{id}/reset-password` | `ROLE_ADMIN` | Active | Unit |
| `GET` | `/api/admin/dashboard` | `ROLE_ADMIN` | **Active & Verified** | `AdminDashboardIntegrationTest` (3 tests), `AdminDashboardControllerTest` (3 tests), `AdminDashboardServiceTest` (8 tests) |
| `GET` | `/api/rooms/{roomNumber}` | Authenticated | Active | Controller / DTO audit (no PII leaked) |
| `POST` | `/api/maintenance` | `ROLE_ADMIN` | **Active & Verified** | `MaintenanceServiceTest` (4 tests) |
| `GET` | `/api/maintenance/{id}` | Admin or Room Owner | **Active & Verified** | `MaintenanceServiceTest` (3 tests) |
| `GET` | `/api/maintenance/room/{roomId}/month/{billingMonth}` | Admin or Room Owner | **Active & Verified** | `MaintenanceServiceTest` (1 test) |
| `GET` | `/api/maintenance/my` | Authenticated | Active | Service / Controller |
| `POST` | `/api/payments` | Authenticated | Active | `PaymentServiceTest` |
| `PUT` | `/api/payments/{id}/proof` | Room Owner | Active | `PaymentServiceTest` (3 tests) |
| `PUT` | `/api/payments/{id}/verify` | `ROLE_ADMIN` | **Active & Verified** | `PaymentServiceTest` (2 tests) |
| `GET` | `/api/payments/my` | Authenticated | Active | `PaymentServiceTest` |
| `GET` | `/api/payments/room/{roomId}` | ~~`ROLE_ADMIN`~~ | **REMOVED** | Removed deprecated temporary mock endpoint (0 usages across app) |
| `POST` | `/api/payment-allocations` | `ROLE_ADMIN` | **Active & Verified** | `PaymentAllocationServiceTest` (2 tests) |
| `GET` | `/api/payment-allocations/payment/{paymentId}` | Admin or Room Owner | **Active & Verified** | `PaymentAllocationServiceTest` (2 tests) |
| `POST` | `/api/cash-payments` | Authenticated | Active | Service |
| `GET` | `/api/cash-payments/{id}` | Admin or Room Owner | **Active & Verified** | `CashPaymentRequestServiceTest` (2 tests) |
| `GET` | `/api/cash-payments/room/{roomId}` | Admin or Room Owner | **Active & Verified** | `CashPaymentRequestServiceTest` (1 test) |
| `GET` | `/api/cash-payments/admin/pending` | `ROLE_ADMIN` | **Active & Verified** | `CashPaymentRequestServiceTest` (2 tests) |
| `POST` | `/api/cash-payments/{id}/verify` | `ROLE_ADMIN` | **Active & Verified** | `CashPaymentRequestServiceTest` (2 tests) |
| `POST` | `/api/cash-payments/{id}/reject` | `ROLE_ADMIN` | **Active & Verified** | `CashPaymentRequestServiceTest` (2 tests) |
| `POST` | `/api/images/upload` | Authenticated | Active | `CloudinaryServiceTest` (4 tests) |

---

## 3. Verified Frontend Screen Inventory

| Screen | File | Data Source | Status |
| :--- | :--- | :--- | :---: |
| `WelcomeScreen` | `screens/WelcomeScreen.js` | Static / Navigation | Complete |
| `LoginScreen` | `screens/LoginScreen.js` | `authService.login()` | Connected |
| `RegisterScreen` | `screens/RegisterScreen.js` | `authService.register()` | Connected |
| `ChangePasswordScreen` | `screens/ChangePasswordScreen.js` | `authService.changePassword()` | Connected |
| `DashboardScreen` | `screens/DashboardScreen.js` | Local Component Props & State | UI complete; live API hookup to `/api/users/me/dashboard` pending |
| `AdminDashboardScreen` | `screens/AdminDashboardScreen.js` | Local Component Mock State | UI complete; API wiring pending (Phase 5) |
| `PaymentScreen` | `screens/PaymentScreen.js` | Mixed / Props | UI complete |
| `OnlinePaymentScreen` | `screens/OnlinePaymentScreen.js` | Cloudinary / API helper | UI complete |
| `CashPaymentScreen` | `screens/CashPaymentScreen.js` | Local / API helper | UI complete |

---

## 4. Test Evidence Log

### Maven Test Run (`mvn test`):
- **Command**: `mvn test` in `c:\Users\DDS\Desktop\Project\app\management_app`
- **Total Tests Run**: 70
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Status**: **BUILD SUCCESS**
- **Execution Time**: ~42 seconds

#### Test Suites Breakdown:
1. `AdminDashboardControllerTest`: 3/3 passed (Unauthenticated 401, Resident Forbidden 403, Admin 200).
2. `AdminDashboardIntegrationTest`: 3/3 passed (Real PostgreSQL aggregation, Resident Forbidden 403, Unauthenticated 401).
3. `AdminDashboardServiceTest`: 8/8 passed (Unit isolation, exemptions, partial allocations, overdue accumulation).
4. `UserControllerTest`: 6/6 passed (Dashboard 401/200, `getUserById` 401/self 200/cross-resident 403/admin 200).
5. `UserServiceAuthorizationTest`: 4/4 passed (Admin access, self access, cross-resident access denied, not found 404).
6. `UserServiceDashboardTest`: 11/11 passed (Isolated dues calculation, exempt rooms, partial payments).
7. `PaymentServiceTest`: 9/9 passed (Screenshot proof updates, `getPaymentById` admin/own-room/cross-room-denied, `verifyPayment` admin/resident-denied).
8. `PaymentAllocationServiceTest`: 4/4 passed (`getAllocationsByPayment` ownership enforcement, `createAllocation` admin allowed/resident denied).
9. `MaintenanceServiceTest`: 8/8 passed (`createMaintenance` admin allowed/resident denied/exempt room/duplicate month, `getMaintenanceById` admin/own-room/cross-room-denied, `getMaintenanceByRoomAndMonth` cross-room-denied).
10. `CashPaymentRequestServiceTest`: 9/9 passed (`getAllPendingRequests` admin/resident-denied, `verifyRequest` admin/resident-denied, `rejectRequest` admin/resident-denied, `getRequestById` own/other-room-denied, `getRequestsByRoom` other-room-denied).
11. `CloudinaryServiceTest`: 4/4 passed.
12. `MaintenanceAppApplicationTests`: 1/1 passed (Spring application context loads).

---

## 5. Security & Authorization Audit Reconciliation

1. **BOLA/IDOR Vulnerabilities**:
   - `GET /api/users/{id}`: Replaced unrestricted lookup with strict ownership check (`currentUser.getRole() == Role.ADMIN || currentUser.getId().equals(user.getId())`). Tested with 8 passing tests.
   - `GET /api/payment-allocations/payment/{paymentId}`: Replaced raw repository query with `paymentService.getPaymentById(paymentId)`, inheriting room ownership enforcement.
2. **Defense-in-Depth for Financial & Admin Operations**:
   - Implemented service-layer `Role.ADMIN` checks in:
     - `MaintenanceService.createMaintenance`
     - `PaymentService.verifyPayment`
     - `CashPaymentRequestService.verifyRequest`
     - `CashPaymentRequestService.rejectRequest`
     - `CashPaymentRequestService.getAllPendingRequests`
     - `PaymentAllocationService.createAllocation`
3. **Deprecated Endpoint Cleanup**:
   - Audited `GET /api/payments/room/{roomId}`: Zero usages across frontend and backend.
   - Removed method from `PaymentController.java`, deleted `getPaymentsByRoom` from `PaymentService.java`, and removed matching pattern in `SecurityConfig.java`. Zero breaking changes on active app.

---

## 6. Discrepancy Report

| Item | Roadmap / Tracker Previous Claim | Actual Codebase Reality | Reconciliation Action Taken |
| :--- | :--- | :--- | :--- |
| `GET /api/payments/room/{roomId}` | Listed as active admin endpoint | Built as temporary pre-auth mock; never consumed by mobile app or backend | Removed endpoint, service method, and security rule. Cleaned up dead code. |
| `MaintenanceService.createMaintenance` | Claimed secured in audit | Only protected at `SecurityConfig` layer; service layer lacked role validation | Added `currentUser.getRole() != Role.ADMIN` check in `MaintenanceService` with 4 unit tests. |
| `DashboardScreen.js` API Hookup | Claimed integrated via `userService.getUserDashboard()` | Screen only uses local props and hardcoded mock data | Corrected status in tracker; flagged as pending frontend task for Phase 2. |
| Test Count | 36 passing tests | 36 passing tests with no unit tests for maintenance, cash requests, allocations, or user ownership | Added 34 focused authorization tests across 5 test suites. Total verified tests: 70/70. |

---

## 7. Blockers & Risks
- **No backend blockers.** All 70 unit, controller, and integration tests pass cleanly with zero errors.
- **Expo Go Metro Bundler**: In the active terminal, `npx expo start -c` was stopped by user (`SIGINT`). Needs to be restarted for physical device testing.
- **Firewall**: Ensure Windows Firewall permits incoming TCP traffic on port 8080 from the local subnet (`10.14.210.x`).

---

## 8. Next Recommended Task
1. Test authentication interactively on a physical phone via Expo Go (`npx expo start -c`).
2. Integrate `DashboardScreen.js` with the verified live backend endpoint `GET /api/users/me/dashboard` (Phase 2).
3. Connect `AdminDashboardScreen.js` to `GET /api/admin/dashboard` (Phase 5).
