# Backend Security and Authorization Audit

**Last Audit Verification:** 2026-10-24  
**Audit Scope:** Authentication, Authorization, Ownership Controls (BOLA/IDOR), Defense-in-Depth, and Deprecated Endpoints  
**Overall Status:** **ALL FINDINGS RESOLVED (Verified with 70/70 Passing Tests)**

---

## 1. Auth / User

### Controller: AuthController
- **HTTP method:** `POST`
- **Path:** `/api/auth/register`
- **Current access:** Public (`permitAll` in `SecurityConfig`)
- **Expected access:** Public
- **Ownership protection:** N/A (Self-registration)
- **Business validation:** Validates if mobile number is already registered; validates room assignment; ensures 1:1 mapping between Room and User; defaults to `Role.USER`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.controller.AuthController.registerUser()`, `com.pravin.maintenance_app.service.UserService.registerUser()`
- **Verified Tests:** Integration test verification in `AdminDashboardIntegrationTest` (db-backed users) & unit contract tests.

### Controller: AuthController
- **HTTP method:** `POST`
- **Path:** `/api/auth/login`
- **Current access:** Public (`permitAll` in `SecurityConfig`)
- **Expected access:** Public
- **Ownership protection:** N/A
- **Business validation:** Validates credentials via `AuthenticationManager`, returns JWT claims (`userId`, `role`, `roomNumber`).
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.controller.AuthController.login()`
- **Verified Tests:** Tested across integration suite and security filter chain.

### Controller: UserController
- **HTTP method:** `GET`
- **Path:** `/api/users/{id}`
- **Current access:** Authenticated
- **Expected access:** `ADMIN` or Room Owner (`userId == currentUser.id`)
- **Ownership protection:** **Enforced.** `UserService.getUserById` checks if the authenticated user has role `ADMIN` or matches `user.getId()`. Unauthorized residents receive `AccessDeniedException` (HTTP 403).
- **Business validation:** Returns 404 (`ResourceNotFoundException`) if user does not exist.
- **Original Finding:** CRITICAL BOLA/IDOR (lack of ownership check).
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.UserService.getUserById(Long id)` (lines 98–108)
- **Verified Tests:**
  - `UserControllerTest.testGetUserById_Unauthenticated_Returns401` (Passed)
  - `UserControllerTest.testGetUserById_SelfAccess_Returns200AndUser` (Passed)
  - `UserControllerTest.testGetUserById_OtherUserForbidden_Returns403` (Passed)
  - `UserControllerTest.testGetUserById_AdminAccess_Returns200AndUser` (Passed)
  - `UserServiceAuthorizationTest.testAdminCanAccessAnyUser` (Passed)
  - `UserServiceAuthorizationTest.testResidentCanAccessOwnUserRecord` (Passed)
  - `UserServiceAuthorizationTest.testResidentCannotAccessOtherUserRecord_ThrowsAccessDeniedException` (Passed)
  - `UserServiceAuthorizationTest.testUserNotFound_ThrowsResourceNotFoundException` (Passed)

---

## 2. Room

### Controller: RoomController
- **HTTP method:** `GET`
- **Path:** `/api/rooms/{roomNumber}`
- **Current access:** Authenticated
- **Expected access:** Authenticated (Directory lookup)
- **Ownership protection:** Public within authenticated realm.
- **Information Disclosure Audit:** Audited `RoomResponse` fields. DTO contains only `id`, `roomNumber`, `status`, `maintenanceExempt`, `createdAt`, `updatedAt`. Contains zero tenant PII, financial amounts, or user IDs.
- **Status:** **RESOLVED** (Safe directory lookup)
- **Code Reference:** `com.pravin.maintenance_app.dto.RoomResponse`, `com.pravin.maintenance_app.controller.RoomController.getRoomByNumber()`

---

## 3. Maintenance

### Controller: MaintenanceController
- **HTTP method:** `POST`
- **Path:** `/api/maintenance`
- **Current access:** `ADMIN`
- **Expected access:** `ADMIN`
- **Ownership protection:** Admin-only operation.
- **Defense-in-Depth:**
  - Endpoint Level: `SecurityConfig` requires `hasRole("ADMIN")`.
  - Service Level: `MaintenanceService.createMaintenance` explicitly checks `currentUser.getRole() != Role.ADMIN` and throws `AccessDeniedException`.
- **Business validation:** Enforces `room.isMaintenanceExempt()`, prevents duplicate records for same room and `billingMonth`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.MaintenanceService.createMaintenance(CreateMaintenanceRequest)` (lines 75–81)
- **Verified Tests:**
  - `MaintenanceServiceTest.testCreateMaintenance_ResidentForbidden_ThrowsAccessDeniedException` (Passed)
  - `MaintenanceServiceTest.testCreateMaintenance_AdminAllowed_SuccessfullyCreates` (Passed)
  - `MaintenanceServiceTest.testCreateMaintenance_ExemptRoom_ThrowsBusinessValidationException` (Passed)
  - `MaintenanceServiceTest.testCreateMaintenance_DuplicateMonth_ThrowsBusinessValidationException` (Passed)

### Controller: MaintenanceController
- **HTTP method:** `GET`
- **Path:** `/api/maintenance/{id}`
- **Current access:** Authenticated
- **Expected access:** `ADMIN` or Room Owner
- **Ownership protection:** **Enforced.** `MaintenanceService.getMaintenanceById` verifies `currentUser.getRole() == Role.ADMIN || currentUser.getRoom().getId().equals(maintenance.getRoom().getId())`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.MaintenanceService.getMaintenanceById(Long)` (lines 34–51)
- **Verified Tests:**
  - `MaintenanceServiceTest.testGetMaintenanceById_AdminAllowed` (Passed)
  - `MaintenanceServiceTest.testGetMaintenanceById_ResidentOwnRoomAllowed` (Passed)
  - `MaintenanceServiceTest.testGetMaintenanceById_ResidentOtherRoomForbidden_ThrowsAccessDeniedException` (Passed)

### Controller: MaintenanceController
- **HTTP method:** `GET`
- **Path:** `/api/maintenance/room/{roomId}/month/{billingMonth}`
- **Current access:** Authenticated
- **Expected access:** `ADMIN` or Room Owner
- **Ownership protection:** **Enforced.** Checks `currentUser.getRole() == Role.ADMIN || currentUser.getRoom().getId().equals(roomId)`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.MaintenanceService.getMaintenanceByRoomAndMonth(Long, YearMonth)` (lines 53–73)
- **Verified Tests:**
  - `MaintenanceServiceTest.testGetMaintenanceByRoomAndMonth_ResidentOtherRoomForbidden_ThrowsAccessDeniedException` (Passed)

### Controller: MaintenanceController
- **HTTP method:** `GET`
- **Path:** `/api/maintenance/my`
- **Current access:** Authenticated
- **Expected access:** Authenticated
- **Ownership protection:** **Enforced.** Derives room directly from authenticated JWT (`currentUser.getRoom().getId()`). Client cannot tamper with room ID.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.MaintenanceService.getMyMaintenance()` (lines 105–112)

---

## 4. Payment

### Controller: PaymentController
- **HTTP method:** `POST`
- **Path:** `/api/payments`
- **Current access:** Authenticated
- **Expected access:** Authenticated
- **Ownership protection:** **Enforced.** Derives room directly from JWT `currentUser.getRoom()`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.PaymentService.createPayment()`

### Controller: PaymentController
- **HTTP method:** `GET`
- **Path:** `/api/payments/my`
- **Current access:** Authenticated
- **Expected access:** Authenticated
- **Ownership protection:** **Enforced.** Derives room directly from JWT `currentUser.getRoom().getId()`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.PaymentService.getMyPayments()`

### Controller: PaymentController
- **HTTP method:** `GET`
- **Path:** `/api/payments/{id}`
- **Current access:** Authenticated
- **Expected access:** `ADMIN` or Room Owner
- **Ownership protection:** **Enforced.** `PaymentService.getPaymentById` checks `currentUser.getRole() != Role.ADMIN && !currentUser.getRoom().getId().equals(payment.getRoom().getId())`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.PaymentService.getPaymentById(Long)` (lines 56–77)
- **Verified Tests:**
  - `PaymentServiceTest.testAdminCanAccessPaymentForAnyRoom` (Passed)
  - `PaymentServiceTest.testResidentCanAccessPaymentForOwnRoom` (Passed)
  - `PaymentServiceTest.testResidentCannotAccessPaymentForDifferentRoom_ThrowsAccessDeniedException` (Passed)

### Controller: PaymentController
- **HTTP method:** `GET`
- **Path:** `/api/payments/room/{roomId}`
- **Original Status:** Marked as temporary ID-based alternative for `/my` before Spring Security was implemented.
- **Action Taken:** **DEPRECATED & REMOVED**. Audited the entire frontend (`frontend-maintenance-app`) and backend (`management_app`): zero callers existed. Removed endpoint method in `PaymentController`, removed unused `getPaymentsByRoom` in `PaymentService`, and removed pattern matcher in `SecurityConfig`.
- **Compatibility Impact:** Zero breaking changes on frontend (resident dashboard uses `/api/payments/my` or `/api/users/me/dashboard`). External manual callers receive standard HTTP 404.
- **Status:** **RESOLVED**

### Controller: PaymentController
- **HTTP method:** `PUT`
- **Path:** `/api/payments/{id}/verify`
- **Current access:** `ADMIN`
- **Expected access:** `ADMIN`
- **Defense-in-Depth:**
  - Endpoint Level: `SecurityConfig` requires `hasRole("ADMIN")`.
  - Service Level: `PaymentService.verifyPayment` checks `currentUser.getRole() != Role.ADMIN` and throws `AccessDeniedException`.
- **Business validation:** Ensures payment status is `PENDING`, updates status to `VERIFIED` and sets `verifiedAt`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.PaymentService.verifyPayment(Long)` (lines 125–144)
- **Verified Tests:**
  - `PaymentServiceTest.testAdminCanVerifyPendingPayment` (Passed)
  - `PaymentServiceTest.testResidentCannotVerifyPayment_ThrowsAccessDeniedException` (Passed)

### Controller: PaymentController
- **HTTP method:** `PATCH`
- **Path:** `/api/payments/{id}/proof`
- **Current access:** Authenticated
- **Expected access:** Room Owner
- **Ownership protection:** **Enforced.** Uses `getPaymentById` which enforces room ownership.
- **Business validation:** Ensures payment status is `PENDING`. Requires at least one proof field (`transactionReference`, `screenshotUrl`, or `screenshotPublicId`).
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.PaymentService.updatePaymentProof()`
- **Verified Tests:** `PaymentServiceTest.testPaymentProofUpdateCanPersistScreenshotPublicId`, `testExistingPaymentWithoutScreenshotPublicIdStillWorks`.

---

## 5. Cash Payment

### Controller: CashPaymentRequestController
- **HTTP method:** `POST`
- **Path:** `/api/cash-payments`
- **Current access:** Authenticated
- **Expected access:** Authenticated
- **Ownership protection:** **Enforced.** Derives room directly from `currentUser.getRoom()`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.CashPaymentRequestService.createRequest()`

### Controller: CashPaymentRequestController
- **HTTP method:** `GET`
- **Path:** `/api/cash-payments/{id}`
- **Current access:** Authenticated
- **Expected access:** `ADMIN` or Room Owner
- **Ownership protection:** **Enforced.** Checks `currentUser.getRole() != Role.ADMIN && !currentUser.getRoom().getId().equals(cashPaymentRequest.getRoom().getId())`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.CashPaymentRequestService.getRequestById(Long)`
- **Verified Tests:**
  - `CashPaymentRequestServiceTest.testGetRequestById_ResidentOwnRoomAllowed` (Passed)
  - `CashPaymentRequestServiceTest.testGetRequestById_ResidentOtherRoomForbidden_ThrowsAccessDeniedException` (Passed)

### Controller: CashPaymentRequestController
- **HTTP method:** `GET`
- **Path:** `/api/cash-payments/room/{roomId}`
- **Current access:** Authenticated
- **Expected access:** `ADMIN` or Room Owner
- **Ownership protection:** **Enforced.** Checks `currentUser.getRole() != Role.ADMIN && !currentUser.getRoom().getId().equals(roomId)`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.CashPaymentRequestService.getRequestsByRoom(Long)`
- **Verified Tests:**
  - `CashPaymentRequestServiceTest.testGetRequestsByRoom_ResidentOtherRoomForbidden_ThrowsAccessDeniedException` (Passed)

### Controller: CashPaymentRequestController
- **HTTP method:** `GET`
- **Path:** `/api/cash-payments/admin/pending`
- **Current access:** `ADMIN`
- **Expected access:** `ADMIN`
- **Defense-in-Depth:**
  - Endpoint Level: `SecurityConfig` requires `hasRole("ADMIN")`.
  - Service Level: `CashPaymentRequestService.getAllPendingRequests` checks `currentUser.getRole() != Role.ADMIN` and throws `AccessDeniedException`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.CashPaymentRequestService.getAllPendingRequests()`
- **Verified Tests:**
  - `CashPaymentRequestServiceTest.testGetAllPendingRequests_AdminAllowed` (Passed)
  - `CashPaymentRequestServiceTest.testGetAllPendingRequests_ResidentForbidden_ThrowsAccessDeniedException` (Passed)

### Controller: CashPaymentRequestController
- **HTTP method:** `POST`
- **Path:** `/api/cash-payments/{id}/verify`
- **Current access:** `ADMIN`
- **Expected access:** `ADMIN`
- **Defense-in-Depth:**
  - Endpoint Level: `SecurityConfig` requires `hasRole("ADMIN")`.
  - Service Level: `CashPaymentRequestService.verifyRequest` checks `currentUser.getRole() != Role.ADMIN` and throws `AccessDeniedException`.
- **Business validation:** Validates `PENDING` status; verifies allocation total equals request amount; generates `Payment` record; creates payment allocations.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.CashPaymentRequestService.verifyRequest()`
- **Verified Tests:**
  - `CashPaymentRequestServiceTest.testVerifyRequest_AdminAllowed_SuccessfullyVerifiesAndAllocates` (Passed)
  - `CashPaymentRequestServiceTest.testVerifyRequest_ResidentForbidden_ThrowsAccessDeniedException` (Passed)

### Controller: CashPaymentRequestController
- **HTTP method:** `POST`
- **Path:** `/api/cash-payments/{id}/reject`
- **Current access:** `ADMIN`
- **Expected access:** `ADMIN`
- **Defense-in-Depth:**
  - Endpoint Level: `SecurityConfig` requires `hasRole("ADMIN")`.
  - Service Level: `CashPaymentRequestService.rejectRequest` checks `currentUser.getRole() != Role.ADMIN` and throws `AccessDeniedException`.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.CashPaymentRequestService.rejectRequest()`
- **Verified Tests:**
  - `CashPaymentRequestServiceTest.testRejectRequest_AdminAllowed` (Passed)
  - `CashPaymentRequestServiceTest.testRejectRequest_ResidentForbidden_ThrowsAccessDeniedException` (Passed)

---

## 6. Payment Allocation

### Controller: PaymentAllocationController
- **HTTP method:** `POST`
- **Path:** `/api/payment-allocations`
- **Current access:** `ADMIN`
- **Expected access:** `ADMIN`
- **Defense-in-Depth:**
  - Endpoint Level: `SecurityConfig` requires `hasRole("ADMIN")`.
  - Service Level: `PaymentAllocationService.createAllocation` checks `currentUser.getRole() != Role.ADMIN` and throws `AccessDeniedException`.
- **Business validation:** Ensures payment is `VERIFIED`, verifies same-room allocation, prevents over-allocation.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.PaymentAllocationService.createAllocation()`
- **Verified Tests:**
  - `PaymentAllocationServiceTest.testCreateAllocation_AdminAllowed_SuccessfullyAllocates` (Passed)
  - `PaymentAllocationServiceTest.testCreateAllocation_ResidentForbidden_ThrowsAccessDeniedException` (Passed)

### Controller: PaymentAllocationController
- **HTTP method:** `GET`
- **Path:** `/api/payment-allocations/payment/{paymentId}`
- **Current access:** Authenticated
- **Expected access:** `ADMIN` or Room Owner
- **Ownership protection:** **Enforced.** Calls `paymentService.getPaymentById(paymentId)`, inheriting its room ownership and `ADMIN` checks.
- **Original Finding:** CRITICAL BOLA (direct repository lookup bypassed ownership checks).
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.PaymentAllocationService.getAllocationsByPayment(Long)`
- **Verified Tests:**
  - `PaymentAllocationServiceTest.testGetAllocationsByPayment_CallsPaymentServiceToEnforceOwnership` (Passed)
  - `PaymentAllocationServiceTest.testGetAllocationsByPayment_WhenPaymentServiceDeniesAccess_ThrowsAccessDeniedException` (Passed)

### Controller: PaymentAllocationController
- **HTTP method:** `GET`
- **Path:** `/api/payment-allocations/maintenance/{maintenanceId}`
- **Current access:** Authenticated
- **Expected access:** `ADMIN` or Room Owner
- **Ownership protection:** **Enforced.** Calls `maintenanceService.getMaintenanceById(maintenanceId)` which enforces ownership and role checks.
- **Status:** **RESOLVED**
- **Code Reference:** `com.pravin.maintenance_app.service.PaymentAllocationService.getAllocationsByMaintenance(Long)`

---

## Summary Matrix

| Finding / Item | Risk Level | Status | Implemented Defense | Verification Tests |
| :--- | :---: | :---: | :--- | :--- |
| `GET /api/users/{id}` BOLA | **CRITICAL** | **RESOLVED** | Role / `currentUser.id` check in `UserService` | `UserControllerTest` (4 tests), `UserServiceAuthorizationTest` (4 tests) |
| `GET /api/payment-allocations/payment/{id}` BOLA | **CRITICAL** | **RESOLVED** | Delegated to `PaymentService.getPaymentById` | `PaymentAllocationServiceTest` (2 tests) |
| `PUT /api/payments/{id}/verify` Defense-in-depth | **HIGH** | **RESOLVED** | `currentUser.getRole() != Role.ADMIN` in service | `PaymentServiceTest` (2 tests) |
| `POST /api/cash-payments/{id}/verify` Defense-in-depth | **HIGH** | **RESOLVED** | `currentUser.getRole() != Role.ADMIN` in service | `CashPaymentRequestServiceTest` (2 tests) |
| `POST /api/cash-payments/{id}/reject` Defense-in-depth | **HIGH** | **RESOLVED** | `currentUser.getRole() != Role.ADMIN` in service | `CashPaymentRequestServiceTest` (2 tests) |
| `POST /api/payment-allocations` Defense-in-depth | **HIGH** | **RESOLVED** | `currentUser.getRole() != Role.ADMIN` in service | `PaymentAllocationServiceTest` (2 tests) |
| `POST /api/maintenance` Defense-in-depth | **HIGH** | **RESOLVED** | `currentUser.getRole() != Role.ADMIN` in service | `MaintenanceServiceTest` (4 tests) |
| `GET /api/cash-payments/admin/pending` Defense-in-depth | **MEDIUM** | **RESOLVED** | `currentUser.getRole() != Role.ADMIN` in service | `CashPaymentRequestServiceTest` (2 tests) |
| `GET /api/payments/room/{roomId}` Deprecated Endpoint | **MEDIUM** | **RESOLVED** | Dead temporary endpoint removed from controller, service & config | Verified 0 usage across frontend/backend; `mvn test` clean |
| `GET /api/rooms/{roomNumber}` Info Disclosure | **LOW** | **RESOLVED** | Verified `RoomResponse` leaks no PII | Code audit of DTO fields |
