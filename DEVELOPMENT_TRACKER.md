# Maintenance App — Development Tracker

Last Updated: 2026-10-26  
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
| **Phase 5** | Admin dashboard and financial overview | **COMPLETE** | Backend `GET /api/admin/dashboard` verified across 14 tests. Frontend `AdminDashboardScreen.js` connected to live API via `api.js` / `adminService.js`, with pull-to-refresh, error retry, loading indicators, empty data handling, and role-based access. |
| **Phase 6** | Admin payment review and allocation management | **COMPLETE** | Backend endpoints verified (`GET /api/payments/admin/pending`, `POST /api/payments/{id}/reject`, `PUT /api/payments/{id}/verify`, `GET /api/cash-payments/admin/pending`, `POST /api/cash-payments/{id}/verify`, `POST /api/cash-payments/{id}/reject`, `POST /api/payment-allocations`, `GET /api/maintenance/room/{roomId}`). Frontend `AdminDashboardScreen.js` integrated with proof screenshot modal, maintenance allocation selection, double-submission guards, and statistics refresh. 46 focused tests pass. |
| **Phase 7** | Resident, room, and account administration | **COMPLETE** | Backend room management (`GET /api/rooms`, `GET /api/rooms/id/{roomId}`, `POST /api/rooms`, `PATCH /api/rooms/{id}/exemption`), user administration (`GET /api/admin/users`, `GET /api/admin/users/room/{roomId}`), and password reset (`POST /api/admin/users/{id}/reset-password`) verified with 33 unit and controller tests. Frontend Tab 2 in `AdminDashboardScreen.js` integrated with live rooms, resident details, filter pills, pull-to-refresh, "+ Add Room" modal, exemption toggle, and password reset modal with temporary password copy. Zero lint errors; Android bundled 1501 modules. |
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
| `GET` | `/api/admin/users` | `ROLE_ADMIN` | **Active & Verified** | `AdminUserControllerTest` (3 tests), `UserServiceAuthorizationTest` (2 tests) |
| `GET` | `/api/admin/users/room/{roomId}` | `ROLE_ADMIN` | **Active & Verified** | `AdminUserControllerTest` (2 tests), `UserServiceAuthorizationTest` (2 tests) |
| `POST` | `/api/admin/users/{id}/reset-password` | `ROLE_ADMIN` | **Active & Verified** | `AdminUserControllerTest` (2 tests), `UserServiceAuthorizationTest` (3 tests) |
| `GET` | `/api/admin/dashboard` | `ROLE_ADMIN` | **Active & Verified** | `AdminDashboardIntegrationTest` (3 tests), `AdminDashboardControllerTest` (3 tests), `AdminDashboardServiceTest` (8 tests) |
| `GET` | `/api/rooms` | Authenticated | **Active & Verified** | `RoomControllerTest` (2 tests), `RoomServiceTest` (1 test) |
| `GET` | `/api/rooms/id/{roomId}` | Authenticated | **Active & Verified** | `RoomServiceTest` (2 tests) |
| `GET` | `/api/rooms/{roomNumber}` | Authenticated | **Active & Verified** | `RoomServiceTest` (1 test) |
| `POST` | `/api/rooms` | `ROLE_ADMIN` | **Active & Verified** | `RoomControllerTest` (2 tests), `RoomServiceTest` (3 tests) |
| `PATCH` | `/api/rooms/{roomId}/exemption` | `ROLE_ADMIN` | **Active & Verified** | `RoomControllerTest` (2 tests), `RoomServiceTest` (2 tests) |
| `POST` | `/api/maintenance` | `ROLE_ADMIN` | **Active & Verified** | `MaintenanceServiceTest` (4 tests) |
| `GET` | `/api/maintenance/{id}` | Admin or Room Owner | **Active & Verified** | `MaintenanceServiceTest` (3 tests) |
| `GET` | `/api/maintenance/room/{roomId}` | Admin or Room Owner | **Active & Verified** | `MaintenanceServiceTest` (2 tests) |
| `GET` | `/api/maintenance/room/{roomId}/month/{billingMonth}` | Admin or Room Owner | **Active & Verified** | `MaintenanceServiceTest` (1 test) |
| `GET` | `/api/maintenance/my` | Authenticated | Active | Service / Controller |
| `POST` | `/api/payments` | Authenticated | Active | `PaymentServiceTest` |
| `GET` | `/api/payments/admin/pending` | `ROLE_ADMIN` | **Active & Verified** | `PaymentControllerTest` (3 tests), `PaymentServiceTest` (2 tests) |
| `PUT` | `/api/payments/{id}/proof` | Room Owner | Active | `PaymentServiceTest` (3 tests) |
| `PUT` | `/api/payments/{id}/verify` | `ROLE_ADMIN` | **Active & Verified** | `PaymentServiceTest` (2 tests), `PaymentControllerTest` (3 tests) |
| `POST` | `/api/payments/{id}/reject` | `ROLE_ADMIN` | **Active & Verified** | `PaymentControllerTest` (3 tests), `PaymentServiceTest` (3 tests) |
| `GET` | `/api/payments/my` | Authenticated | Active | `PaymentServiceTest` |
| `GET` | `/api/payments/room/{roomId}` | Admin or Room Owner | **Active & Verified** | `PaymentServiceTest` (3 tests), `PaymentControllerTest` (4 tests) |
| `POST` | `/api/payment-allocations` | `ROLE_ADMIN` | **Active & Verified** | `PaymentAllocationServiceTest` (2 tests) |
| `GET` | `/api/payment-allocations/payment/{paymentId}` | Admin or Room Owner | **Active & Verified** | `PaymentAllocationServiceTest` (2 tests) |
| `GET` | `/api/payment-allocations/maintenance/{maintenanceId}` | Admin or Room Owner | **Active & Verified** | `PaymentAllocationServiceTest` (2 tests) |
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
| `AdminDashboardScreen` | `screens/AdminDashboardScreen.js` | `getAdminDashboard()`, `getPendingCashRequests()`, `getPendingOnlinePayments()`, `verifyCashRequest()`, `verifyOnlinePayment()`, `rejectCashRequest()`, `rejectOnlinePayment()`, `createPaymentAllocation()`, `getMaintenanceByRoom()`, `getAllRooms()`, `createRoom()`, `updateRoomExemption()`, `getAllResidents()`, `resetResidentPassword()` | **Connected & Verified (Phase 5, 6 & 7 Complete)** |
| `PaymentScreen` | `screens/PaymentScreen.js` | Mixed / Props | UI complete |
| `OnlinePaymentScreen` | `screens/OnlinePaymentScreen.js` | Cloudinary / API helper | UI complete |
| `CashPaymentScreen` | `screens/CashPaymentScreen.js` | Local / API helper | UI complete |

---

## 4. Test Evidence Log

### Phase 7 Backend Test Verification (`mvn test`):
- **Command 1**: `./mvnw test '-Dtest=RoomServiceTest,UserServiceAuthorizationTest'` in `c:\Users\DDS\Desktop\Project\app\management_app`
  - Total Tests Run: 20
  - Failures: 0, Errors: 0, Skipped: 0
  - Status: **BUILD SUCCESS** (Execution time: ~13.7s)
  - Breakdown:
    - `RoomServiceTest`: 9/9 passed (getAllRooms sorted, getRoomById, getRoomByRoomNumber, admin create room, resident create forbidden, duplicate room validation, admin update exemption, resident update exemption forbidden).
    - `UserServiceAuthorizationTest`: 11/11 passed (admin reset resident password, resident reset forbidden, admin cannot reset admin password, admin get all users, resident get all users forbidden, admin get user by room id, resident get user by room id forbidden, own record access, other record forbidden, user not found).
- **Command 2**: `./mvnw test '-Dtest=AdminUserControllerTest,RoomControllerTest'` in `c:\Users\DDS\Desktop\Project\app\management_app`
  - Total Tests Run: 13
  - Failures: 0, Errors: 0, Skipped: 0
  - Status: **BUILD SUCCESS** (Execution time: ~38.9s)
  - Breakdown:
    - `AdminUserControllerTest`: 7/7 passed (`GET /api/admin/users` 401/403/200, `GET /api/admin/users/room/{id}` 403/200, `POST /api/admin/users/{id}/reset-password` 403/200).
    - `RoomControllerTest`: 6/6 passed (`GET /api/rooms` 401/200, `POST /api/rooms` 403/201, `PATCH /api/rooms/{id}/exemption` 403/200).

### Total Phase 7 Verified Tests: 33/33 Passed (Cumulative Suite Tests: 133 tests)

### Phase 7 Frontend Verification:
- **Command 1**: `npm run lint` in `c:\Users\DDS\Desktop\Project\app\frontend-maintenance-app`
  - Result: Zero lint errors or warnings. Clean build.
- **Command 2**: `npx expo export --platform android` in `c:\Users\DDS\Desktop\Project\app\frontend-maintenance-app`
  - Result: Android bundled 1501 modules successfully with 0 errors.

---

## 5. Security & Authorization Audit Reconciliation

1. **BOLA/IDOR Vulnerabilities & Account Administration**:
   - `GET /api/admin/users`: Protected by `hasRole("ADMIN")` in `SecurityConfig` and `currentUser.getRole() == Role.ADMIN` check in `UserService.getAllUsers`. Residents receive 403 Forbidden.
   - `GET /api/admin/users/room/{roomId}`: Protected by `hasRole("ADMIN")` and service-layer check. Residents cannot inspect accounts assigned to rooms.
   - `POST /api/admin/users/{userId}/reset-password`: Protected by `hasRole("ADMIN")` and service-layer check. Admin accounts cannot have their passwords reset via this endpoint (`BusinessValidationException`).
2. **Room Management Authorization**:
   - `POST /api/rooms`: Strictly restricted to `ROLE_ADMIN`. Residents receive 403 Forbidden. Uniqueness enforced against duplicate room numbers.
   - `PATCH /api/rooms/{roomId}/exemption`: Strictly restricted to `ROLE_ADMIN`. Residents receive 403 Forbidden.
   - Initial 40 rooms preserved (101–110, 201–210, 301–310, 401–410; Room 207 exempt). Room number identity maintained without adding unnecessary entities.
3. **Defense-in-Depth & Credential Hygiene**:
   - Both web filter security (`SecurityConfig`) and service-layer authorization checks are enforced on all Phase 7 admin operations.
   - Temporary password generation uses `SecureRandom`.
   - Temporary passwords are never logged to console or client storage.
   - Sets `mustChangePassword=true` forcing password update upon next login.

---

## 6. Discrepancy Report

| Item | Roadmap / Tracker Previous Claim | Actual Codebase Reality | Reconciliation Action Taken |
| :--- | :--- | :--- | :--- |
| Room Listing & Creation Endpoints | Absent in `RoomController` | `RoomController` only had `GET /api/rooms/{roomNumber}` | Added `GET /api/rooms`, `GET /api/rooms/id/{roomId}`, `POST /api/rooms`, and `PATCH /api/rooms/{id}/exemption` with `ADMIN` role checks and 15 tests. |
| User Listing & Room Lookup | Absent in `AdminUserController` | `AdminUserController` only had password reset | Added `GET /api/admin/users` and `GET /api/admin/users/room/{roomId}` with `ADMIN` role checks and 14 tests. |
| Service Defense-in-Depth | `UserService.adminResetPassword` lacked `currentUser.getRole() == Role.ADMIN` check | Only checked target user role | Added service-layer current user role check and null-safe room number resolution. |
| DTO `roomId` in `UserResponse` | Missing `roomId` field | Frontend needed `roomId` to correlate resident accounts with room entities | Added `roomId` with getter/setter to `UserResponse` and mapped in `UserMapper`. |
| Room Entity Lifecycle | `createdAt`/`updatedAt` lacked `@PrePersist`/`@PreUpdate` | New room creation via API risked null timestamps | Added `@PrePersist` and `@PreUpdate` lifecycle methods to `Room` entity. |

---

## 7. Blockers & Risks
- **No backend blockers.** All unit, controller, and integration tests pass cleanly with zero errors.
- **Expo Go Metro Bundler**: Start bundler with `npx expo start -c` when testing on a physical phone.
- **Firewall**: Ensure Windows Firewall permits incoming TCP traffic on port 8080 from the local subnet (`10.14.210.x`).

---

## 8. Next Recommended Task
1. Integrate `DashboardScreen.js` with the verified live backend endpoint `GET /api/users/me/dashboard` (Phase 2).
2. Complete end-to-end acceptance flow on a physical device in Expo Go for admin room/account administration, online payment review, and resident login.
3. Proceed to Phase 8 release preparations.
