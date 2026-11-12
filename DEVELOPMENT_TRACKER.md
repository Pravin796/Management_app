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
| **Phase 3** | Online payments and proof submission | **COMPLETE** | Refactored into a single-step multipart proof submission: `OnlinePaymentScreen.js` keeps image locally, validates UTR and screenshot, and sends both via `submitPaymentProof()` (`POST /api/payments/{id}/submit-proof`). Backend verifies ownership & PENDING status, streams to Cloudinary, persists proof in DB, and compensates with Cloudinary rollback if DB fails. Separate upload button removed. |
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
| `POST` | `/api/payments/{id}/submit-proof` | Room Owner | **Active & Verified** | Single multipart/form-data proof submission (UTR + screenshot file) with Cloudinary upload & compensation rollback |
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
| `OnlinePaymentScreen` | `screens/OnlinePaymentScreen.js` | `createPayment()`, `submitPaymentProof()` | **Connected & Verified (Phase 3 Single-Step Multipart Refactor)** |
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
| Button Touch & Pager Clipping | Both `DashboardScreen.js` and `AdminDashboardScreen.js` used `width * 4` horizontal translateX transforms inside `overflow: hidden`, which caused React Native / Android ViewGroup touch dispatcher to clip offscreen layout coordinates, dropping touches on Profile tab (Logout), Payments tab, and subviews | Replaced translated row containers with direct active tab rendering (`{activeTab === '...' && ...}`), ensuring all tabs, buttons, inputs, and modals receive touches at (0, 0) with zero clipping | **Fixed & Verified** |
| Missing Web Logout Dialog Handler | React Native Web ignores `Alert.alert` buttons parameter, preventing logout confirmation callback execution | Created cross-platform `confirmAction` dialog in `src/utils/alert.js` with browser `window.confirm` fallback; added quick "Sign Out" pill to Profile cards | **Fixed & Verified** |
| Unregistered Payment Routes in Stack Navigator | `App.js` lacked `OnlinePayment` and `CashPayment` routes, and `PaymentScreen` lacked navigation callbacks for online/cash payments | Registered `OnlinePayment` and `CashPayment` in `App.js` Stack Navigator; connected `PaymentScreen` props `onNavigateOnlinePayment` and `onNavigateCashPayment`; added top in-screen Back buttons | **Fixed & Verified** |

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

### E. Single-Step Multipart Payment Proof Submission Refactor (2026-11-02)
1. **Frontend Streamlining (`OnlinePaymentScreen.js` & `api.js`)**:
   - Eliminated the separate "Upload" button, independent `/api/uploads/image` call, and warning banner.
   - Image picker stores local URI with full local preview; allows replacing via "Choose Another" or removing via "Remove".
   - Collects UTR / Transaction ID and validates required fields.
   - On "Submit for Verification", checks required UTR and screenshot, acquires pending payment ID, and dispatches a single multipart request via `submitPaymentProof(paymentId, { transactionReference, imageUri })` using native `XMLHttpRequest` streaming.
   - Double submission is prevented with disabled state and loading indicators; on failure, user input and screenshot are preserved for instant retry without re-picking.
2. **Backend Multipart Endpoint (`PaymentController.java` & `PaymentService.java`)**:
   - Added `POST /api/payments/{id}/submit-proof` consuming `multipart/form-data` with `@RequestParam("transactionReference")` and `@RequestParam("file") MultipartFile`.
   - Security: Enforces authenticated user, checks room ownership (`getPaymentById(id)`), and validates `PENDING` status before any upload.
   - Cloudinary Integration: Uploads to folder `payment_proofs` with file type (`image/*`) and size ($\le$ 10MB) validation only during final submission.
   - Rollback Compensation: If database persistence fails after Cloudinary upload succeeds, catches the error and executes `cloudinaryService.deleteImage(publicId)` before propagating the exception.
   - Cloudinary credentials remain strictly encapsulated on the server.

---

## 10. Phase 9: Cross-Method Payment Locking and Concurrency Control (2026-11-09)

### A. Problem Statement & Objectives
1. **Unconstrained Multiple Submissions**: Previously, residents could submit both a cash payment request and an online payment simultaneously, or multiple payments in rapid succession. When Room 101 had both a cash payment and an online UPI payment pending while maintaining ₹0 outstanding dues, verifying the second payment resulted in backend error `"Allocation amount exceeds remaining maintenance amount"`.
2. **Admin Filter Responsiveness**: On Android devices, horizontal filter pills (`All / Pending`, `Online`, `Cash`, `Verified`) in `AdminDashboardScreen` suffered from touch interception due to `nestedScrollEnabled={true}` inside parent scroll views and case sensitivity differences, causing the filter to not display items when tapped.
3. **Core Mandate**:
   - Enforce a strict single-pending-submission constraint across both `ONLINE` and `CASH` methods per room identity.
   - Concurrency serialization via pessimistic database row-level locking on `Room`.
   - Complete frontend locking of `PaymentScreen`, `OnlinePaymentScreen`, and `CashPaymentScreen` while awaiting admin review.
   - Automatic unlock and dues recalculation upon Admin verification or rejection.

### B. Implementation Details

1. **Backend Concurrency & Locking Architecture**:
   - `RoomRepository`: Added pessimistic write lock query:
     ```java
     @Lock(LockModeType.PESSIMISTIC_WRITE)
     @Query("SELECT r FROM Room r WHERE r.id = :id")
     Optional<Room> findByIdForUpdate(@Param("id") Long id);
     ```
     This executes `SELECT ... FROM rooms WHERE id = ? FOR UPDATE` inside Spring's transaction boundary, serializing concurrent online payment and cash request creation per room at the database level.
   - `PaymentRepository`:
     - Added `existsSubmittedPaymentByRoomId(Long roomId)` to identify payments that are genuinely submitted for admin review (`status == PENDING AND (transactionReference IS NOT NULL OR screenshotUrl IS NOT NULL)`), distinguishing incomplete payment intents from submitted proof.
     - Added `findSubmittedPaymentsByRoomId(Long roomId)` and `findIncompletePaymentsByRoomId(Long roomId)`.
   - `PaymentService`:
     - `createPayment`: Acquires pessimistic lock on Room; checks if cash request is pending (`existsByRoomIdAndStatus(..., PENDING)`) or submitted online payment exists (`existsSubmittedPaymentByRoomId(...)`). If an incomplete payment intent exists, reuses/updates its amount and method rather than failing or creating duplicates.
     - `submitPaymentProof` & `updatePaymentProof`: Locks Room row; validates no pending cash request and no other submitted online payment; preserves Cloudinary upload and rollback compensation on DB error.
   - `CashPaymentRequestService`:
     - `createRequest`: Acquires pessimistic lock on Room; checks `cashPaymentRequestRepository.existsByRoomIdAndStatus(room.getId(), PENDING)` and `paymentRepository.existsSubmittedPaymentByRoomId(room.getId())`, throwing `BusinessValidationException` if either is awaiting verification.
     - Added `getMyCashRequests()` returning all cash requests for the authenticated resident's room.
   - `CashPaymentRequestController`:
     - Added `@GetMapping("/my")` endpoint for authenticated residents to retrieve their cash payment requests.
   - `UserDashboardResponse` & `UserService`:
     - Added `PendingPaymentDto pendingPayment` to `UserDashboardResponse` containing `id`, `amount`, `method` (`ONLINE` / `CASH`), `status` (`PENDING`), `createdAt`, and `transactionReference`.
     - In `getUserDashboard()`, populates `pendingPayment` whenever a submitted online payment or pending cash payment request exists for that room.

2. **Frontend Payment Screen Locking**:
   - `PaymentScreen.js`:
     - Evaluates `isPaymentLocked` directly from `dashboardData?.pendingPayment != null`.
     - Evaluates `isDuesCleared` when `!isPaymentLocked && outstandingBalance <= 0`.
     - When locked, renders a prominent verification banner:
       *"Your payment has been submitted successfully and is awaiting admin verification. You cannot make another payment until the administrator reviews your current payment."*
       Displays pending payment method, amount, submission date, status badge (`AWAITING VERIFICATION`), and reference/UTR.
     - Hero card button displays `"Under Review"` and alerts the resident if clicked.
     - Both Quick Action tiles (`Online Payment` and `Cash Payment`) are visually dimmed (`opacity: 0.55`) and disabled.
     - When dues are cleared (`isDuesCleared`), displays `"All Dues Cleared"` banner, hero button shows `"All Paid"`, and quick actions are disabled.
   - `DashboardScreen.js`:
     - In `handleOpenOnlinePayment` and `handleOpenCashPayment`, verifies locking state and outstanding dues before opening subviews.
     - On switching to the payments tab, automatically invokes `fetchDashboardData(true)` to refresh payment locking state from the server.
   - `OnlinePaymentScreen.js` & `CashPaymentScreen.js`:
     - Added `isPaymentLocked` prop; disables amount editing, disables submission buttons, and displays verification lock warning if accessed while a submission is awaiting review.
   - `src/services/api.js`:
     - Added `getMyCashRequests = async () => (await api.get('/cash-payments/my')).data;`.

3. **Admin Payment Reviews Filter Fix & Proof Image Loader (`AdminDashboardScreen.js`)**:
   - Removed `nestedScrollEnabled={true}` from the horizontal filter pills `ScrollView`, resolving Android touch-event cancellation where taps were intercepted by the vertical container.
   - Added `keyboardShouldPersistTaps="always"` and normalized filter comparisons (`pType` and `pStatus` case normalization).
   - In the Verify modal, added pre-validation for allocations to ensure payments cannot be allocated to maintenance records that are already cleared (`remaining <= 0`).
   - Added `ProofImageViewer` with a centered `ActivityIndicator` loading overlay ("Loading proof image...") while the receipt proof screenshot downloads from cloud storage, preventing a blank container. Included error recovery with "Retry" and "Open Link" buttons.

### C. Route & Contract Additions
| Action | HTTP Method | Endpoint | Access | Response |
|---|---|---|---|---|
| Get My Cash Requests | `GET` | `/api/cash-payments/my` | Authenticated Resident / Admin | `200 OK` `List<CashPaymentRequestResponse>` |
| Get User Dashboard | `GET` | `/api/users/dashboard` | Authenticated Resident / Admin | `200 OK` Includes `pendingPayment` DTO when awaiting review |
| Get All Admin Payments | `GET` | `/api/payments/admin/all` | `ROLE_ADMIN` | `200 OK` `List<Payment>` (PENDING, VERIFIED, REJECTED) |
| Get Rejected Payments | `GET` | `/api/payments/admin/rejected` | `ROLE_ADMIN` | `200 OK` `List<Payment>` |
| Get All Admin Cash Requests | `GET` | `/api/cash-payments/admin/all` | `ROLE_ADMIN` | `200 OK` `List<CashPaymentRequest>` |
| Get Rejected Cash Requests | `GET` | `/api/cash-payments/admin/rejected` | `ROLE_ADMIN` | `200 OK` `List<CashPaymentRequest>` |
| Upload Profile Picture | `POST` | `/api/users/me/profile-picture` | Authenticated Resident / Admin | `200 OK` `UserResponse` with `profilePictureUrl` |
| Remove Profile Picture | `DELETE` | `/api/users/me/profile-picture` | Authenticated Resident / Admin | `200 OK` `UserResponse` |
| Get My Profile | `GET` | `/api/users/me` | Authenticated Resident / Admin | `200 OK` `UserResponse` with `profilePictureUrl` |

---

## 7. Payment Eligibility, Auto-Allocation & Admin Overwrite Resolution (2026-11-12)

### A. Root Cause of Admin Payment List Overwrite
1. **Frontend Mutation of Existing Pending Rows**: In `OnlinePaymentScreen.js`, if `createPayment` returned an error that a pending payment already existed, the `catch` block queried `/payments/my`, retrieved the existing pending payment ID, and called `submitPaymentProof(existingPayment.id, ...)`, mutating the previous payment proof and reference in-place.
2. **Backend Row Recycling**: In `PaymentService.createPayment`, the service previously called `findIncompletePaymentsByRoomId` and recycled existing payment records rather than inserting new entities.
3. **Missing Admin History Endpoints**: The admin review endpoints only fetched `PENDING` and `VERIFIED` records; `REJECTED` payments vanished completely from view.
4. **Hardcoded Filter in AdminDashboardScreen**: In `AdminDashboardScreen.js`, the "All" filter was hardcoded to `return pStatus === 'pending'`, hiding all verified and rejected historical records.
5. **Dynamic / Unstable React Keys**: The admin list used dynamic keys (`online_${p.id}` when pending, changing to `verified_${p.id}` upon verification), which caused list churning and component re-mount issues across state changes.

### B. Implementation Details
1. **Cross-Method Payment Submission Restrictions**:
   - Backend `PaymentService.createPayment` and `CashPaymentRequestService.createRequest` strictly enforce cross-method payment intent locking: a resident cannot submit an online payment or declare cash if an active payment of either method is awaiting verification (`status = PENDING`).
   - If outstanding maintenance is zero or the room is exempt, new payment initiation is rejected.
   - The payable amount cannot exceed actual outstanding dues.

2. **Monthly Eligibility & Outstanding Balance Calculation**:
   - `MaintenanceService.calculateOutstandingBalance(roomId)` computes total outstanding maintenance strictly from active monthly maintenance records minus verified `PaymentAllocation` records.
   - Pending, rejected, and failed payments do not count as paid.
   - `UserService.getUserDashboard` delegates directly to `MaintenanceService` for consistent financial accounting.

3. **Oldest-First Auto-Allocation**:
   - `PaymentAllocationService.autoAllocatePaymentToOldestMaintenance(Payment payment)` automatically allocates verified payments to the oldest outstanding months first.
   - Prevents over-allocation: the allocated amount per month cannot exceed that month's unpaid balance.
   - Sets monthly maintenance status to `PAID` once fully settled, or `PARTIALLY_PAID` if a remaining balance exists.

4. **Independent Record Creation & Stable React Keys**:
   - Removed payment row-recycling. Every submission produces a new database row with a unique ID.
   - Backend exposes `/api/payments/admin/all` and `/api/cash-payments/admin/all` with role-based admin security.
   - Admin UI maps items using stable identifiers (`pay-${id}` and `cash-${id}`).
   - Filter pills allow filtering by `All`, `Pending`, `Online`, `Cash`, `Verified`, and `Rejected`.
   - Item cards display visual status badges and color-coded banners for all states (including red banner for Rejected payments with admin remarks).

5. **Network Connectivity & IP Alignment**:
   - Diagnosed host DHCP IP address (`192.168.1.80`) and aligned `frontend-maintenance-app/.env` (`EXPO_PUBLIC_API_URL=http://192.168.1.80:8080/api`).

---

## 8. Resident Profile Photo Upload & Admin Visibility (2026-11-12)

### A. Architecture & Requirements Fulfillment
1. **Post-Registration Profile Photo Management**:
   - Registration flow (`RegisterScreen.js` and `POST /api/auth/register`) strictly preserved without image upload.
   - Authenticated residents can upload and update their profile picture anytime from the Profile tab in `DashboardScreen.js`.
2. **Cloudinary Integration & Automatic Cleanup**:
   - Uploads to folder `profile_pictures` via `POST /api/users/me/profile-picture` (`MultipartFile file`).
   - Persists `profile_picture_url` and `profile_picture_public_id` on the `User` entity.
   - Automatically cleans up the previous Cloudinary asset when replacing or removing a profile photo.
   - Rollback compensation deletes newly uploaded asset if database persistence fails.
3. **Database Migration**:
   - Flyway migration `V8__add_profile_picture_to_users.sql` adds `profile_picture_url` and `profile_picture_public_id` to the `users` table.
4. **Admin Room Visibility**:
   - `UserResponse` includes `profilePictureUrl`.
   - `AdminDashboardScreen.js` Tab 2 (Rooms list): each occupied room row renders the resident's profile picture thumbnail next to the room number.
   - In Room Details modal (`selectedRoomDetail`): displays the resident's full circular profile picture avatar with graceful fallback to default icon if vacant or no picture uploaded.
5. **Universal Header Display**:
   - `ScreenHeader.js` across all resident tabs (Home, Payments, History, Profile) immediately reflects the uploaded profile picture.




