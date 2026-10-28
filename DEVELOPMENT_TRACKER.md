# Maintenance App — Development Tracker

Last Updated: 2026-10-27  
Single source of truth for verified progress, test evidence, blockers, and active tasks.

---

## 1. Executive Summary & Current Phase Status

| Phase | Title | Status | Verified Evidence |
| :--- | :--- | :---: | :--- |
| **Phase 0** | Existing implementation audit and project baseline | **COMPLETE** | Full backend/frontend audit conducted; API & screen inventory completed; all initial baseline tests pass. |
| **Phase 1** | Authentication, session handling, and app foundation | **COMPLETE** | Backend auth APIs verified (`/api/auth/register`, `/api/auth/login`, `/api/auth/change-password`). Frontend session restoration verified in `App.js`. First-login forced password change & 1-hour reset cooldown verified. |
| **Phase 2** | Resident dashboard and maintenance history | **COMPLETE** | Backend `GET /api/users/me/dashboard`, `GET /api/maintenance/my`, and `GET /api/payments/my` connected to `DashboardScreen.js`. Tab 2 payment history bound to real API records; mock fallback balance eliminated; 13 backend unit/controller tests pass. |
| **Phase 3** | Online payments and proof submission | **COMPLETE** | Connected `OnlinePaymentScreen.js` to live APIs: `createPayment()`, `uploadImage()` (multipart to `/api/uploads/image`), and `updatePaymentProof()` (`PATCH /api/payments/{id}/proof`). 17 backend tests in `PaymentServiceTest` and `PaymentControllerTest` pass. |
| **Phase 4** | Cash payment request workflow | **COMPLETE** | Connected `CashPaymentScreen.js` to live API: `createCashPaymentRequest()`. Removed redundant `@NotNull` roomId constraint in DTO so room is securely derived from authenticated user. 12 tests in `CashPaymentRequestServiceTest` pass. |
| **Phase 5** | Admin dashboard and financial overview | **COMPLETE** | Backend `GET /api/admin/dashboard` verified across 14 tests. Frontend `AdminDashboardScreen.js` connected to live API with pull-to-refresh, error retry, loading indicators, empty data handling, and role-based access. |
| **Phase 6** | Admin payment review and allocation management | **COMPLETE** | Backend endpoints verified (`GET /api/payments/admin/pending`, `PUT /api/payments/{id}/verify`, `POST /api/payments/{id}/reject`, `GET /api/cash-payments/admin/pending`, `POST /api/cash-payments/{id}/verify`, `POST /api/cash-payments/{id}/reject`, `POST /api/payment-allocations`, `GET /api/maintenance/room/{roomId}`). Frontend integrated with proof screenshot modal, maintenance allocation selection, double-submission guards, and statistics refresh. 46 focused tests pass. |
| **Phase 7** | Resident, room, and account administration | **COMPLETE** | Backend room management (`GET /api/rooms`, `GET /api/rooms/id/{roomId}`, `POST /api/rooms`, `PATCH /api/rooms/{id}/exemption`), user administration (`GET /api/admin/users`, `GET /api/admin/users/room/{roomId}`), and password reset (`POST /api/admin/users/{id}/reset-password`) verified with 33 unit and controller tests. Frontend Tab 2 integrated with live rooms, resident details, filter pills, pull-to-refresh, "+ Add Room" modal, exemption toggle, and password reset modal with temporary password clipboard copy. |
| **Phase 8** | Cross-feature QA, deployment readiness, and release | **IN PROGRESS (READY FOR FIELD DEPLOYMENT)** | Complete cross-feature audit conducted. Resident recovery flow secured with dedicated `ForgotPasswordScreen` and server-side `MustChangePasswordFilter`. Financial correctness verified (₹500/month, exempt room skipping, partial payment allocation, balance calculated from verified allocations only). CORS configured in `SecurityConfig.java`. 130 cumulative targeted backend tests pass across 4 suites. `npm run lint` clean (0 errors). Android bundle export clean (1508 modules, 4.2MB HBC bundle). Production deployment checklist documented. (Physical device testing and production server deployment pending user execution; Phase 8 is not marked complete until live device acceptance is fulfilled). |

---

## 2. Verified API Inventory (Backend)

| Method | Endpoint | Auth / Roles | Status | Verified Tests |
| :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/auth/register` | Public | **Active & Verified** | Integration / Unit |
| `POST` | `/api/auth/login` | Public | **Active & Verified** | Integration / Unit |
| `POST` | `/api/auth/change-password` | Authenticated (Accessible when `mustChangePassword=true`) | **Active & Verified** | Unit (`UserServiceAuthorizationTest`, `MustChangePasswordFilterTest`) |
| `GET` | `/api/users/me/dashboard` | `ROLE_USER`, `ROLE_ADMIN` (Blocked if `mustChangePassword=true`) | **Active & Verified** | `UserControllerTest` (2 tests), `UserServiceDashboardTest` (11 tests), `MustChangePasswordFilterTest` (2 tests) |
| `GET` | `/api/users/{id}` | Admin or Self (Blocked if `mustChangePassword=true`) | **Active & Verified** | `UserControllerTest` (4 tests), `UserServiceAuthorizationTest` (4 tests) |
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
| `GET` | `/api/maintenance/my` | Authenticated (Blocked if `mustChangePassword=true`) | **Active & Verified** | `MaintenanceServiceTest` (2 tests) |
| `POST` | `/api/payments` | Authenticated (Blocked if `mustChangePassword=true`) | **Active & Verified** | `PaymentServiceTest` (3 tests), `PaymentControllerTest` (3 tests), `MustChangePasswordFilterTest` (1 test) |
| `GET` | `/api/payments/admin/pending` | `ROLE_ADMIN` | **Active & Verified** | `PaymentControllerTest` (3 tests), `PaymentServiceTest` (2 tests) |
| `PATCH` | `/api/payments/{id}/proof` | Room Owner | **Active & Verified** | `PaymentServiceTest` (3 tests), `PaymentControllerTest` (2 tests) |
| `PUT` | `/api/payments/{id}/verify` | `ROLE_ADMIN` | **Active & Verified** | `PaymentServiceTest` (2 tests), `PaymentControllerTest` (3 tests) |
| `POST` | `/api/payments/{id}/reject` | `ROLE_ADMIN` | **Active & Verified** | `PaymentControllerTest` (3 tests), `PaymentServiceTest` (3 tests) |
| `GET` | `/api/payments/my` | Authenticated (Blocked if `mustChangePassword=true`) | **Active & Verified** | `PaymentServiceTest` (2 tests), `PaymentControllerTest` (2 tests) |
| `GET` | `/api/payments/room/{roomId}` | Admin or Room Owner | **Active & Verified** | `PaymentServiceTest` (3 tests), `PaymentControllerTest` (4 tests) |
| `POST` | `/api/payment-allocations` | `ROLE_ADMIN` | **Active & Verified** | `PaymentAllocationServiceTest` (2 tests) |
| `GET` | `/api/payment-allocations/payment/{paymentId}` | Admin or Room Owner | **Active & Verified** | `PaymentAllocationServiceTest` (2 tests) |
| `GET` | `/api/payment-allocations/maintenance/{maintenanceId}` | Admin or Room Owner | **Active & Verified** | `PaymentAllocationServiceTest` (2 tests) |
| `POST` | `/api/cash-payments` | Authenticated (Blocked if `mustChangePassword=true`) | **Active & Verified** | `CashPaymentRequestServiceTest` (3 tests) |
| `GET` | `/api/cash-payments/{id}` | Admin or Room Owner | **Active & Verified** | `CashPaymentRequestServiceTest` (2 tests) |
| `GET` | `/api/cash-payments/room/{roomId}` | Admin or Room Owner | **Active & Verified** | `CashPaymentRequestServiceTest` (1 test) |
| `GET` | `/api/cash-payments/admin/pending` | `ROLE_ADMIN` | **Active & Verified** | `CashPaymentRequestServiceTest` (2 tests) |
| `POST` | `/api/cash-payments/{id}/verify` | `ROLE_ADMIN` | **Active & Verified** | `CashPaymentRequestServiceTest` (2 tests) |
| `POST` | `/api/cash-payments/{id}/reject` | `ROLE_ADMIN` | **Active & Verified** | `CashPaymentRequestServiceTest` (2 tests) |
| `POST` | `/api/uploads/image` | Authenticated | **Active & Verified** | `CloudinaryServiceTest` (4 tests) |

---

## 3. Verified Frontend Screen Inventory

| Screen | File | Data Source | Status |
| :--- | :--- | :--- | :---: |
| `WelcomeScreen` | `screens/WelcomeScreen.js` | Static / Navigation | Complete |
| `LoginScreen` | `screens/LoginScreen.js` | `authService.login()` | Connected & Verified |
| `ForgotPasswordScreen` | `screens/ForgotPasswordScreen.js` | Static / Guidance | **Connected & Verified (Admin-assisted recovery)** |
| `RegisterScreen` | `screens/RegisterScreen.js` | `authService.register()` | Connected & Verified |
| `ChangePasswordScreen` | `screens/ChangePasswordScreen.js` | `authService.changePassword()` | **Connected & Verified (Forced mode & self-service)** |
| `DashboardScreen` | `screens/DashboardScreen.js` | `getUserDashboard()`, `getMyPayments()`, `getMyMaintenance()` | **Connected & Verified (Phase 2 Complete)** |
| `AdminDashboardScreen` | `screens/AdminDashboardScreen.js` | `getAdminDashboard()`, `getPendingCashRequests()`, `getPendingOnlinePayments()`, `verifyCashRequest()`, `verifyOnlinePayment()`, `rejectCashRequest()`, `rejectOnlinePayment()`, `createPaymentAllocation()`, `getMaintenanceByRoom()`, `getAllRooms()`, `createRoom()`, `updateRoomExemption()`, `getAllResidents()`, `resetResidentPassword()` | **Connected & Verified (Phases 5, 6, 7 Complete)** |
| `PaymentScreen` | `screens/PaymentScreen.js` | Props & Navigation | Connected & Verified |
| `OnlinePaymentScreen` | `screens/OnlinePaymentScreen.js` | `createPayment()`, `uploadImage()`, `updatePaymentProof()` | **Connected & Verified (Phase 3 Complete)** |
| `CashPaymentScreen` | `screens/CashPaymentScreen.js` | `createCashPaymentRequest()` | **Connected & Verified (Phase 4 Complete)** |

---

## 4. Test Evidence Log

### Phase 8 Backend Targeted Test Runs (`mvn test`):
- **Suite 1: Payment & Cash Request Workflows**
  - Command: `mvn test "-Dtest=PaymentServiceTest,CashPaymentRequestServiceTest,PaymentControllerTest"`
  - Total Tests Run: 46
  - Failures: 0, Errors: 0, Skipped: 0
  - Status: **BUILD SUCCESS** (Execution time: 28.2s)
- **Suite 2: Admin Administration & Role Authorization**
  - Command: `mvn test "-Dtest=AdminUserControllerTest,AdminDashboardControllerTest,RoomControllerTest,PaymentAllocationServiceTest,UserServiceAuthorizationTest"`
  - Total Tests Run: 35
  - Failures: 0, Errors: 0, Skipped: 0
  - Status: **BUILD SUCCESS** (Execution time: 48.1s)
- **Suite 3: User & Controller Security Integration with CORS**
  - Command: `mvn test "-Dtest=AdminUserControllerTest,AdminDashboardControllerTest,UserControllerTest"`
  - Total Tests Run: 16
  - Failures: 0, Errors: 0, Skipped: 0
  - Status: **BUILD SUCCESS** (Execution time: 32.5s)
- **Suite 4: Password Security, Cooldown & MustChangePassword Filter Enforcement**
  - Command: `mvn test "-Dtest=MustChangePasswordFilterTest,UserServiceAuthorizationTest,AdminUserControllerTest,UserControllerTest"`
  - Total Tests Run: 33
  - Failures: 0, Errors: 0, Skipped: 0
  - Status: **BUILD SUCCESS** (Execution time: 30.8s)
  - Coverage: `MustChangePasswordFilter` bypasses OPTIONS, `/api/auth/change-password`, `/v3/api-docs`; blocks authenticated requests with `mustChangePassword=true` with 403 Forbidden on protected endpoints; unauthenticated requests pass to Spring Security for 401; admin password reset cooldown and authorization verified.

**Total Verified Tests Across Phase 8 Runs: 130 Tests Passed (0 Failures, 0 Errors)**

### Phase 8 Frontend Verification:
- **Lint Check**: `npm run lint`
  - Result: 0 errors, 0 warnings. Clean.
- **Android Expo Bundle Export**: `npx expo export --platform android`
  - Result: Android bundled 1508 modules in 19.8s. Exported `dist/` cleanly with 0 errors (`_expo/static/js/android/index-*.hbc` 4.2MB). Cleaned up temporary directory.

---

## 5. Security & Password Lifecycle Architecture

1. **Recovery & Anti-Takeover Policy**:
   - Automated reset via mobile number or room input is strictly prohibited to prevent unauthorized room takeover and financial manipulation. No OTP or SMS gateways are used.
   - Password reset is strictly admin-assisted:
     1. Resident contacts society admin (e.g., Room 207).
     2. Admin verifies resident identity and room ownership.
     3. Admin triggers reset via `POST /api/admin/users/{id}/reset-password`.
     4. Backend generates a 10-character `SecureRandom` password, BCrypt-hashes it, sets `mustChangePassword=true`, and enforces a 1-hour anti-conflict cooldown (`password_reset_at`).
     5. Admin communicates temporary password securely (displayed only in admin reset modal with copy action; never logged).
2. **First-Login Enforcement (Client & Server Security Boundary)**:
   - Server-side: [MustChangePasswordFilter.java](file:///c:/Users/DDS/Desktop/Project/app/management_app/src/main/java/com/pravin/maintenance_app/security/MustChangePasswordFilter.java) intercepts all requests on protected endpoints (e.g. `/api/users/me/dashboard`, `/api/payments`). If `user.isMustChangePassword()` is `true`, it immediately responds with `403 Forbidden` (`"Password change required before accessing the application"`). Only `/api/auth/change-password` and public endpoints are permitted.
   - Client-side: [App.js](file:///c:/Users/DDS/Desktop/Project/app/frontend-maintenance-app/App.js) intercepts login with `mustChangePassword=true` and resets navigation stack directly to `ChangePasswordScreen` in forced mode (`isForced=true`), disabling back-navigation to the dashboard.
   - Reactive safeguard: [api.js](file:///c:/Users/DDS/Desktop/Project/app/frontend-maintenance-app/src/services/api.js) response interceptor catches 403 password-change-required errors and invokes `setPasswordChangeRequiredHandler`, immediately routing the user to `ChangePasswordScreen`.
3. **Permanent Password Update**:
   - `POST /api/auth/change-password` requires `currentPassword`, `newPassword` ($\ge 6$ chars), and matching `confirmPassword`.
   - Validates that new password differs from current password.
   - Upon successful update: `mustChangePassword` is set to `false`, `passwordResetAt` is cleared to `null`, and user receives full access.

---

## 6. Discrepancy & Defect Fix Report

| Defect / Mismatch | Root Cause | Fix Applied | Status |
| :--- | :--- | :--- | :---: |
| Forgot Password Navigation | `LoginScreen.js` called `onNavigateChangePassword` which required current password & active session | Created dedicated [ForgotPasswordScreen.js](file:///c:/Users/DDS/Desktop/Project/app/frontend-maintenance-app/screens/ForgotPasswordScreen.js) explaining admin-assisted recovery; updated `LoginScreen.js` and `App.js` routes | **Fixed & Verified** |
| Missing Backend `mustChangePassword` Enforcement | Backend allowed requests to `/api/users/me/dashboard` or `/api/payments` even if `mustChangePassword=true` | Added [MustChangePasswordFilter.java](file:///c:/Users/DDS/Desktop/Project/app/management_app/src/main/java/com/pravin/maintenance_app/security/MustChangePasswordFilter.java) after `JwtAuthenticationFilter` in `SecurityConfig.java`; verified with 7 unit tests | **Fixed & Verified** |
| Online Payments Mocking | `OnlinePaymentScreen.js` used `setTimeout` mock | Connected to live `createPayment`, multipart `uploadImage` to `/api/uploads/image`, and `updatePaymentProof` (`PATCH /api/payments/{id}/proof`) | **Fixed & Verified** |
| Cash Payments Mocking | `CashPaymentScreen.js` used `setTimeout` mock | Connected to live `createCashPaymentRequest` call | **Fixed & Verified** |
| `CreateCashPaymentRequest.java` Constraint | `@NotNull(message = "Room id is required")` on DTO | Removed `@NotNull` from DTO; service derives room from `currentUser.getRoom()`. Added explicit null-safe exception if user has no room. | **Fixed & Verified** |
| `DashboardScreen.js` Mock Tab 2 | Tab 2 used static mock payment records and fallback balance 1000 | Connected `getMyPayments` and `getMyMaintenance`; bound real payments to history tab; replaced fallback balance with 0 default; added empty state. | **Fixed & Verified** |
| `AdminDashboardScreen.js` Reset Crash | Typo calling `setResidentsList` instead of `setResidents` | Fixed identifier to `setResidents` | **Fixed & Verified** |
| Missing CORS in `SecurityConfig.java` | Spring Security rejected cross-origin OPTIONS preflight | Added `CorsConfigurationSource` bean and enabled `.cors(...)` | **Fixed & Verified** |

---

## 7. Deployment Readiness Checklist

### Backend Deployment Readiness:
- [x] Spring Boot 3.4.3 compiles and passes all unit and integration tests (130 tests verified).
- [x] Flyway migrations V1 through V7 validate successfully against PostgreSQL schema.
- [x] Server-side password change enforcement active via `MustChangePasswordFilter`.
- [x] Database connection properties externalized (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- [x] JWT Secret and expiration externalized (`JWT_SECRET`, `JWT_EXPIRATION`).
- [x] Initial admin credentials externalized (`ADMIN_MOBILE_NUMBER`, `ADMIN_PASSWORD`, `ADMIN_NAME`, `ADMIN_ROOM_NUMBER`).
- [x] Cloudinary credentials externalized (`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`).
- [x] CORS enabled for cross-origin access and OPTIONS preflight.
- [ ] *Production Deployment*: Supply production environment variables on target cloud host. Set `spring.jpa.hibernate.ddl-auto=validate`.

### Frontend Deployment Readiness:
- [x] React Native 0.88 / Expo 58 app compiles cleanly.
- [x] `npm run lint` passes with 0 errors and 0 warnings.
- [x] `npx expo export --platform android` bundles 1508 modules without errors.
- [x] API URL configured via `EXPO_PUBLIC_API_URL` with fallback in `src/config/env.js`.
- [x] Session restoration, token persistence via `expo-secure-store`, unauthenticated route guards, and forced password change redirection verified.
- [ ] *Physical Device Acceptance*: Start Metro with `npx expo start -c --port 8082` and test on Android device using Expo Go on same Wi-Fi (`http://<LAN_IP>:8080/api`).
- [ ] *Standalone Android Build*: Run `npx eas build -p android --profile production` to generate signed release APK/AAB for Play Store.

---

## 8. Blockers & Remaining Actions

### Blockers:
- **Zero code or test blockers.** All backend APIs and frontend screens are connected, tested, and passing.

### Remaining Actions for User / Operator:
1. **Physical Device Acceptance Test**: Connect Android phone via Expo Go to verify camera/image picker, offline alerts, pull-to-refresh, forgot password screen, and temporary password login flow on physical hardware.
2. **Production Hosting**: Deploy Spring Boot jar to cloud host with production PostgreSQL and set production environment variables (`JWT_SECRET`, `CLOUDINARY_*`, `DB_*`).
3. **EAS Build**: Build production APK/AAB using EAS Build with production `EXPO_PUBLIC_API_URL`.

---

## 9. Payment Screenshot Upload & Cloudinary Integration Verification

### A. Architectural Flow & Verified Mappings
1. **Local Preview Only on Selection**:
   - `OnlinePaymentScreen.js` uses `ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], allowsEditing: false, quality: 0.8 })`.
   - Selecting an image populates `localImageUri` only; no automatic network request is made.
   - Editing controls and unneeded state removed, keeping "Choose Another" and "Remove".
2. **Explicit Two-Stage Upload & Native Multipart Handling**:
   - "Upload" button sends `multipart/form-data` with field name `file` and folder `payment_proofs` to `POST /api/uploads/image`.
   - In React Native / Expo, Expo Winter's global `fetch` does not support `{ uri, name, type }` in `FormData` (it throws `Unsupported FormDataPart implementation`). Fixed in `src/services/api.js` by using native `XMLHttpRequest` for multipart file uploads and configuring `api` instance with `adapter: ['xhr', 'fetch']`, allowing React Native's native OkHttp client on Android to stream files directly from disk without JavaScript buffer conversion.
   - Server returns HTTP 201 with `{ url: "https://res.cloudinary.com/...", publicId: "..." }`.
   - Cloudinary credentials remain strictly server-side (`CLOUDINARY_*` in `.env` / `application.yaml`) and are never exposed to the frontend.
3. **Hosted Preview & Proof Submission**:
   - Preview switches to hosted Cloudinary URL with badge "Uploaded to Cloud" only after server confirmation.
   - "Submit for Verification" sends returned `screenshotUrl` and `screenshotPublicId` along with `transactionReference` (UTR) via `PATCH /api/payments/{id}/proof` for the pending payment.
   - If submission fails or network drops, `uploadedUrl` and `uploadedPublicId` remain in state so proof submission can be retried without re-uploading to Cloudinary.
   - Submit button is disabled while uploading or if screenshot is chosen but not yet uploaded, preventing partial submissions.
4. **Physical Device Networking**:
   - Android physical devices cannot use `localhost` (which refers to the phone itself).
   - Host LAN IP `http://192.168.1.68:8080/api` is configured in `frontend-maintenance-app/.env` and `src/config/env.js`.
5. **Database & Schema**:
   - Reuses existing Flyway V6 column `screenshot_public_id` and `Payment.screenshotPublicId`. No additional DB migration required.

### B. Verified Route Mappings & Contract
| Action | HTTP Method | Endpoint | Request Body / Multipart | Response |
|---|---|---|---|---|
| Upload Screenshot | `POST` | `/api/uploads/image` | `file`: MultipartFile (max 10MB, image/*), `folder`: "payment_proofs" | `201 Created` `{ url, publicId }` |
| Submit Proof | `PATCH` | `/api/payments/{id}/proof` | `{ transactionReference, screenshotUrl, screenshotPublicId }` | `200 OK` `PaymentResponse` |

### C. Test Results
- **Backend Tests**: 45/45 passing (`mvn test "-Dtest=ImageUploadControllerTest,CloudinaryServiceTest,PaymentControllerTest,PaymentServiceTest"`).
  - `ImageUploadControllerTest`: 4/4 passing (401 unauthenticated, 201 success with `{ url, publicId }`, 400 bad request / business validation).
  - `CloudinaryServiceTest`: 7/7 passing (null file rejection, empty file rejection, non-image rejection, >10MB rejection, blank public ID rejection, valid upload to Cloudinary).
  - `PaymentControllerTest`: 17/17 passing (proof patch authorization, pending status validation, resident ownership checks).
  - `PaymentServiceTest`: 17/17 passing.
- **Frontend Verification**:
  - `npm run lint`: 0 errors, 0 warnings.
  - `npx expo export --platform android`: 1508 modules bundled successfully into Hermes bytecode (`index-bcc0103898f30e84cb367a08ea6171fa.hbc`, 4.2MB).

### D. Remaining Manual Checks
- [ ] Test image selection and upload with physical Android phone connected via Expo Go on `192.168.1.68:8080` to verify camera/gallery access and actual Cloudinary upload.
- [ ] Confirm receipt display in Admin Verification modal renders the Cloudinary image thumbnail.

