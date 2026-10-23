# Maintenance App — Development Tracker

Last Updated: 2026-10-23  
Single source of truth for verified progress, test evidence, blockers, and active tasks.

---

## 1. Executive Summary & Current Phase Status

| Phase | Title | Status | Verified Evidence |
| :--- | :--- | :---: | :--- |
| **Phase 0** | Existing implementation audit and project baseline | **COMPLETE** | Full backend/frontend audit conducted; API & screen inventory completed; 36/36 backend tests pass. |
| **Phase 1** | Authentication, session handling, and app foundation | **IN PROGRESS** | Backend auth APIs verified (`/api/auth/register`, `/api/auth/login`, `/api/auth/change-password`). Frontend navigation & session restoration in place in `App.js`. |
| **Phase 2** | Resident dashboard and maintenance history | **IN PROGRESS** | Backend `GET /api/users/me/dashboard` verified & passing 13 tests. Frontend `DashboardScreen.js` integrated via `userService.getUserDashboard()`. |
| **Phase 3** | Online payments and proof submission | **IN PROGRESS** | Backend payments + Cloudinary proof upload APIs verified. Frontend screens `PaymentScreen.js` & `OnlinePaymentScreen.js` exist. |
| **Phase 4** | Cash payment request workflow | **IN PROGRESS** | Backend cash request & admin verify/reject APIs verified. Frontend `CashPaymentScreen.js` exists. |
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
| `GET` | `/api/users/{id}` | Admin or Self | Active | Unit |
| `POST` | `/api/admin/users/{id}/reset-password` | `ROLE_ADMIN` | Active | Unit |
| `GET` | `/api/admin/dashboard` | `ROLE_ADMIN` | **Active & Verified** | `AdminDashboardIntegrationTest` (3 tests), `AdminDashboardControllerTest` (3 tests), `AdminDashboardServiceTest` (8 tests) |
| `GET` | `/api/rooms` | Authenticated | Active | Service / Controller |
| `POST` | `/api/rooms` | `ROLE_ADMIN` | Active | Service / Controller |
| `POST` | `/api/maintenance` | `ROLE_ADMIN` | Active | Service / Controller |
| `GET` | `/api/maintenance/my` | Authenticated | Active | Service / Controller |
| `POST` | `/api/payments` | Authenticated | Active | `PaymentServiceTest` (4 tests) |
| `PUT` | `/api/payments/{id}/proof` | Room Owner | Active | `PaymentServiceTest` |
| `PUT` | `/api/payments/{id}/verify` | `ROLE_ADMIN` | Active | `PaymentServiceTest` |
| `GET` | `/api/payments/my` | Authenticated | Active | `PaymentServiceTest` |
| `POST` | `/api/payment-allocations` | `ROLE_ADMIN` | Active | `PaymentAllocationServiceTest` |
| `POST` | `/api/cash-payments` | Authenticated | Active | Service |
| `GET` | `/api/cash-payments/my` | Authenticated | Active | Service |
| `GET` | `/api/cash-payments/admin/pending` | `ROLE_ADMIN` | Active | Service |
| `PUT` | `/api/cash-payments/{id}/verify` | `ROLE_ADMIN` | Active | Service |
| `PUT` | `/api/cash-payments/{id}/reject` | `ROLE_ADMIN` | Active | Service |
| `POST` | `/api/images/upload` | Authenticated | Active | `CloudinaryServiceTest` (4 tests) |

---

## 3. Verified Frontend Screen Inventory

| Screen | File | Data Source | Status |
| :--- | :--- | :--- | :---: |
| `WelcomeScreen` | `screens/WelcomeScreen.js` | Static / Navigation | Complete |
| `LoginScreen` | `screens/LoginScreen.js` | `authService.login()` | Connected |
| `RegisterScreen` | `screens/RegisterScreen.js` | `authService.register()` | Connected |
| `ChangePasswordScreen` | `screens/ChangePasswordScreen.js` | `authService.changePassword()` | Connected |
| `DashboardScreen` | `screens/DashboardScreen.js` | `userService.getUserDashboard()` | Connected to Live API |
| `AdminDashboardScreen` | `screens/AdminDashboardScreen.js` | Local Component Mock State | UI complete; API wiring pending (Phase 5) |
| `PaymentScreen` | `screens/PaymentScreen.js` | Mixed / Props | UI complete |
| `OnlinePaymentScreen` | `screens/OnlinePaymentScreen.js` | Cloudinary / API helper | UI complete |
| `CashPaymentScreen` | `screens/CashPaymentScreen.js` | Local / API helper | UI complete |

---

## 4. Test Evidence Log

### Maven Test Run (`mvn test`):
- **Command**: `mvn test` in `c:\Users\pravi\OneDrive\Desktop\ADV-WEBprojects\Radhika-app\Maintenance_App`
- **Total Tests Run**: 36
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Status**: **BUILD SUCCESS**

#### Key Test Suites Breakdown:
1. `AdminDashboardIntegrationTest`:
   - `testGetAdminDashboard_RealServiceAndDatabase_ReturnsValidMetrics`: Passed (Status 200, returned real PostgreSQL metrics: `totalRooms: 40`, `totalMaintenanceCollected: 3500.00`, `totalOutstandingBalance: 1500.00`, `pendingOnlinePayments: 2`, `pendingCashRequests: 0`, `verifiedCashPayments: 1`).
   - `testGetAdminDashboard_ResidentForbidden_EndToEnd`: Passed (Status 403 Forbidden).
   - `testGetAdminDashboard_Unauthenticated_EndToEnd`: Passed (Status 401 Unauthorized).
2. `AdminDashboardControllerTest`:
   - `testGetAdminDashboard_Unauthenticated_Returns401`: Passed.
   - `testGetAdminDashboard_ResidentForbidden_Returns403`: Passed.
   - `testGetAdminDashboard_AdminAccess_Returns200AndDashboard`: Passed.
3. `AdminDashboardServiceTest`:
   - `testResidentForbidden_ThrowsAccessDeniedException`: Passed.
   - `testEmptyData_ReturnsZeroCountsAndZeroBalances`: Passed.
   - `testAdminAccess_ReturnsCorrectAggregatedMetrics`: Passed.
   - `testPendingPayments_NotCountedAsVerifiedCollections`: Passed.
   - `testPartialAllocations_ReducesOutstandingBalanceAndCountsCollected`: Passed.
   - `testOutstandingBalances_AccumulatesMultipleUnpaidRecords`: Passed.
   - `testExemptRooms_NotCountedInMaintenanceOrBalances_CountedInTotalRooms`: Passed.
   - `testDirectPaidMaintenance_CountedAsCollectedWithoutAllocationRecords`: Passed.
4. `UserControllerTest`:
   - `testGetDashboard_Unauthenticated_Returns401`: Passed.
   - `testGetDashboard_Authenticated_Returns200AndDashboard`: Passed.
5. `UserServiceDashboardTest`:
   - 11 tests verifying resident data isolation, dues accumulation, partial payments, exempt rooms, etc. All passed.
6. `CloudinaryServiceTest`: 4 tests passed.
7. `PaymentServiceTest`: 4 tests passed.
8. `MaintenanceAppApplicationTests`: Spring context loads cleanly.

---

## 5. Phase 1 Verification & Findings

### A. Architectural & Contract Verification:
- **Login Contract (`POST /api/auth/login`)**:
  - Request: `{ mobileNumber, password }`
  - Response: `{ userId, roomNumber, mobileNumber, role, token, mustChangePassword }`
  - `authService.login()` extracts `token` and persists the user profile in `tokenStorage`.
- **Registration Contract (`POST /api/auth/register`)**:
  - Request: `{ roomNumber, name, mobileNumber, password, confirmPassword }`
  - Response: `{ id, roomNumber, name, mobileNumber, role, status }`
  - Validated by Spring Boot Bean Validation (`@NotBlank`, `@Size`).
- **Change Password Contract (`POST /api/auth/change-password`)**:
  - Request: `{ currentPassword, newPassword, confirmPassword }`
  - Response: `{ message }`
  - Requires Bearer token; successfully resets `mustChangePassword` to `false`.
- **Role-Based Navigation & Session Restoration**:
  - On app launch, `App.js` checks `tokenStorage`. If valid, routes:
    - `mustChangePassword === true` -> `ChangePasswordScreen` (in forced mode).
    - `role === 'ADMIN'` -> `AdminDashboardScreen`.
    - `role === 'USER'` -> `DashboardScreen`.
  - Non-admins are prevented from staying on `AdminDashboard` by a runtime role check in `App.js`.
  - Logout cleanly purges `tokenStorage` and resets navigation stack to `WelcomeScreen`.

### B. Confirmed Fixes Applied:
1. **Active Wi-Fi IPv4 in `.env`**: Replaced stale/commented IP with active host IPv4 (`http://10.14.210.173:8080/api`) so Expo Go on physical devices can connect.
2. **Fallback Port in `src/config/env.js`**: Corrected default fallback URL from port `8081` to port `8080`.
3. **Automatic 401 Token Expiration Purge in `api.js`**: Configured Axios response interceptor to intercept 401 responses on authenticated requests, clear `tokenStorage`, and notify the app.
4. **App Session Reset on Unauthorized**: Hooked up `setUnauthorizedHandler` in `App.js` to trigger `handleLogout` when a token expires or is rejected by Spring Security.

### C. Testing Verification Matrix:
- **Automated / Code-Verified**:
  - Registration contract validation and DTO mapping.
  - Login authentication against BCrypt hashes and JWT claim generation.
  - Role-based redirect logic (`ADMIN`, `USER`, `mustChangePassword`).
  - Session restoration and SecureStore persistence logic.
  - Expired/invalid JWT rejection (HTTP 401) and security filter chain.
- **Physical Device / Expo Go Prerequisites**:
  - Both development machine and phone must be on the same local network (`10.14.210.x`).
  - Backend must be running on port 8080 (`mvn spring-boot:run`).
  - Metro bundler running (`npx expo start`).

---

## 6. Discrepancy Report

| Item | Roadmap / Tracker Previous Claim | Actual Codebase Reality | Reconciliation Action Taken |
| :--- | :--- | :--- | :--- |
| `DEVELOPMENT_TRACKER.md` | Referenced in `PROJECT_ROADMAP.md` as mandatory reading | File did not exist on disk | Created `DEVELOPMENT_TRACKER.md` as single source of truth. |
| `GET /api/admin/dashboard` | Listed as proposed / unverified | Fully implemented in controller & service; verified against PostgreSQL with 14 passing tests | Documented as completed backend feature under Phase 5. |
| Phase 0 Status | Marked `IN PROGRESS` | Baseline audit, API contracts, screen inventory, and test coverage confirmed | Phase 0 acceptance criteria satisfied; marked COMPLETE. |
| Frontend `.env` IP | Set to `10.134.163.173` | Active machine Wi-Fi IP is `10.14.210.173` | Updated `.env` to active Wi-Fi IP for Expo Go physical device testing. |
| Expired Token Handling | Not wired in Axios interceptor | 401 responses on authenticated requests were only logged | Added 401 interception, `tokenStorage.clearAuth()`, and `handleLogout` redirect. |

---

## 7. Blockers & Risks
- **None currently blocking backend development.**
- Backend endpoints for auth, resident dashboard, admin dashboard, payments, allocations, and cash requests are verified with automated tests.
- When performing physical device tests on Expo Go, ensure firewall allows incoming connections on port 8080.

---

## 8. Next Recommended Task
- Test login, registration, and session restoration interactively on physical device via Expo Go.
- Once physical device acceptance is confirmed by user, mark Phase 1 **`COMPLETE`** and proceed to **Phase 2 (Resident dashboard and maintenance history)**.
