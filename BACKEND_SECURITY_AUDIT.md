# Backend Security and Authorization Audit

## 1. Auth / User

### Controller: AuthController
- **HTTP method:** POST
- **Path:** `/api/auth/register`
- **Current access:** Public (`permitAll`)
- **Expected access:** Public
- **Ownership protection:** N/A
- **Business validation:** Validates if mobile number is already registered. Validates if the room already has a registered account. Ensures 1:1 mapping between Room and User. Hardcodes `Role.USER`.
- **Risk:** None. Handled correctly according to business rules.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: AuthController
- **HTTP method:** POST
- **Path:** `/api/auth/login`
- **Current access:** Public (`permitAll`)
- **Expected access:** Public
- **Ownership protection:** N/A
- **Business validation:** Authenticates credentials and generates JWT.
- **Risk:** None. Handled correctly.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: UserController
- **HTTP method:** GET
- **Path:** `/api/users/{id}`
- **Current access:** Authenticated (Any role)
- **Expected access:** ADMIN or Room Owner (Current User)
- **Ownership protection:** **NONE.** `UserService.getUserById` simply fetches the user from the repository by ID without checking if the ID matches the currently authenticated user.
- **Business validation:** Returns 404 if not found.
- **Risk:** Broken Object Level Authorization (BOLA/IDOR). Any authenticated user can access the personal information of any other user in the system by guessing their user ID.
- **Recommended change:** Add an ownership check in `UserService.getUserById` to ensure that either the current user's role is `ADMIN`, or `currentUser.getId().equals(id)`.
- **Priority:** **CRITICAL**

---

## 2. Room

### Controller: RoomController
- **HTTP method:** GET
- **Path:** `/api/rooms/{roomNumber}`
- **Current access:** Authenticated (Any role)
- **Expected access:** Authenticated (Directory access) or Room Owner
- **Ownership protection:** None.
- **Business validation:** Returns 404 if not found.
- **Risk:** Information disclosure. If the `RoomResponse` contains sensitive information (e.g., owner names, arrears, user references), this is a BOLA vulnerability. If it only returns basic static room info (like block/floor), it is acceptable as a directory lookup.
- **Recommended change:** Ensure `RoomResponse` does not leak sensitive information. If it does, restrict this endpoint to `ADMIN` or the specific room owner.
- **Priority:** LOW

---

## 3. Maintenance

### Controller: MaintenanceController
- **HTTP method:** POST
- **Path:** `/api/maintenance`
- **Current access:** ADMIN (Protected by `SecurityConfig`)
- **Expected access:** ADMIN
- **Ownership protection:** N/A (Admin only)
- **Business validation:** Validates that the room is not maintenance-exempt. Prevents duplicate maintenance records for the same room and billing month. Hardcodes amount from `application.yml` properties.
- **Risk:** None. Handled correctly.
- **Recommended change:** Add `Role.ADMIN` check in `MaintenanceService` for defense-in-depth.
- **Priority:** LOW

### Controller: MaintenanceController
- **HTTP method:** GET
- **Path:** `/api/maintenance/{id}`
- **Current access:** Authenticated
- **Expected access:** ADMIN or Room Owner
- **Ownership protection:** **Secure.** `MaintenanceService.getMaintenanceById` explicitly checks if `currentUser.getRole() == ADMIN` or if `currentUser.getRoom().getId()` matches the maintenance record's room ID.
- **Business validation:** Returns 404 if not found.
- **Risk:** None. Handled correctly.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: MaintenanceController
- **HTTP method:** GET
- **Path:** `/api/maintenance/room/{roomId}/month/{billingMonth}`
- **Current access:** Authenticated
- **Expected access:** ADMIN or Room Owner
- **Ownership protection:** **Secure.** `MaintenanceService` verifies ownership against `roomId`.
- **Business validation:** Returns 404 if not found.
- **Risk:** None. Handled correctly.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: MaintenanceController
- **HTTP method:** GET
- **Path:** `/api/maintenance/my`
- **Current access:** Authenticated
- **Expected access:** Authenticated
- **Ownership protection:** **Secure.** Derives the `roomId` directly from the authenticated JWT via `CurrentUserService`.
- **Business validation:** None needed.
- **Risk:** None.
- **Recommended change:** None.
- **Priority:** LOW

---

## 4. Payment

### Controller: PaymentController
- **HTTP method:** POST
- **Path:** `/api/payments`
- **Current access:** Authenticated
- **Expected access:** Authenticated
- **Ownership protection:** **Secure.** Derives room directly from `CurrentUserService`. Client cannot spoof `roomId`.
- **Business validation:** Prevents creating a new payment if a `PENDING` payment already exists for the room.
- **Risk:** None.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: PaymentController
- **HTTP method:** GET
- **Path:** `/api/payments/my`
- **Current access:** Authenticated
- **Expected access:** Authenticated
- **Ownership protection:** **Secure.** Derives room directly from `CurrentUserService`.
- **Business validation:** None needed.
- **Risk:** None.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: PaymentController
- **HTTP method:** GET
- **Path:** `/api/payments/{id}`
- **Current access:** Authenticated
- **Expected access:** ADMIN or Room Owner
- **Ownership protection:** **Secure.** `PaymentService.getPaymentById` checks if the user is `ADMIN` or the owner of the payment's room.
- **Business validation:** Returns 404 if not found.
- **Risk:** None.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: PaymentController
- **HTTP method:** GET
- **Path:** `/api/payments/room/{roomId}`
- **Current access:** ADMIN (Protected by `SecurityConfig`)
- **Expected access:** ADMIN
- **Ownership protection:** Relies entirely on `SecurityConfig`. Service layer does not check roles.
- **Business validation:** None.
- **Risk:** This is marked as a "Temporary ID-based alternative for /my". Relying only on `SecurityConfig` makes it brittle.
- **Recommended change:** Remove this endpoint entirely since `/api/payments/my` is fully implemented and ADMIN can use an admin-specific endpoint if needed.
- **Priority:** MEDIUM

### Controller: PaymentController
- **HTTP method:** PUT
- **Path:** `/api/payments/{id}/verify`
- **Current access:** ADMIN (Protected by `SecurityConfig`)
- **Expected access:** ADMIN
- **Ownership protection:** N/A
- **Business validation:** Ensures payment status is `PENDING`. Updates status to `VERIFIED`.
- **Risk:** The service layer (`PaymentService.verifyPayment`) does **not** check if the user is an ADMIN. It relies 100% on `SecurityConfig`. If `SecurityConfig` is ever misconfigured, a normal user could verify payments because `getPaymentById` allows room owners to access their own payments.
- **Recommended change:** Add an explicit `if (currentUser.getRole() != Role.ADMIN) throw AccessDeniedException(...)` inside `PaymentService.verifyPayment` for defense-in-depth.
- **Priority:** HIGH

### Controller: PaymentController
- **HTTP method:** PATCH
- **Path:** `/api/payments/{id}/proof`
- **Current access:** Authenticated
- **Expected access:** Room Owner
- **Ownership protection:** **Secure.** Uses `getPaymentById` which enforces ownership.
- **Business validation:** Ensures status is `PENDING`. Requires at least one proof field to be provided.
- **Risk:** None.
- **Recommended change:** None.
- **Priority:** LOW

---

## 5. Cash Payment

### Controller: CashPaymentRequestController
- **HTTP method:** POST
- **Path:** `/api/cash-payments`
- **Current access:** Authenticated
- **Expected access:** Authenticated
- **Ownership protection:** **Secure.** Derives room directly from `CurrentUserService`.
- **Business validation:** Checks for existing pending cash payment requests.
- **Risk:** None.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: CashPaymentRequestController
- **HTTP method:** GET
- **Path:** `/api/cash-payments/{id}`
- **Current access:** Authenticated
- **Expected access:** ADMIN or Room Owner
- **Ownership protection:** **Secure.** Service layer checks `ADMIN` or room ownership.
- **Business validation:** Returns 404 if not found.
- **Risk:** None.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: CashPaymentRequestController
- **HTTP method:** GET
- **Path:** `/api/cash-payments/room/{roomId}`
- **Current access:** Authenticated
- **Expected access:** ADMIN or Room Owner
- **Ownership protection:** **Secure.** Service layer checks `ADMIN` or room ownership against `roomId`.
- **Business validation:** None.
- **Risk:** None.
- **Recommended change:** None.
- **Priority:** LOW

### Controller: CashPaymentRequestController
- **HTTP method:** GET
- **Path:** `/api/cash-payments/admin/pending`
- **Current access:** ADMIN (Protected by `SecurityConfig`)
- **Expected access:** ADMIN
- **Ownership protection:** N/A
- **Business validation:** Fetches all pending.
- **Risk:** Service layer does not check for `Role.ADMIN`, relying entirely on `SecurityConfig`.
- **Recommended change:** Add `Role.ADMIN` check in service layer.
- **Priority:** MEDIUM

### Controller: CashPaymentRequestController
- **HTTP method:** POST
- **Path:** `/api/cash-payments/{id}/verify`
- **Current access:** ADMIN (Protected by `SecurityConfig`)
- **Expected access:** ADMIN
- **Ownership protection:** N/A
- **Business validation:** Validates request is `PENDING`. Strictly validates that the sum of `allocations` exactly matches the cash payment `amount`. Creates the actual `Payment` record and executes `PaymentAllocationService.createAllocation`.
- **Risk:** The service layer does **not** check for `Role.ADMIN`. It uses `getRequestById` which allows room owners access. If `SecurityConfig` fails, a user could verify their own cash request.
- **Recommended change:** Add explicit `Role.ADMIN` check in `verifyRequest`.
- **Priority:** HIGH

### Controller: CashPaymentRequestController
- **HTTP method:** POST
- **Path:** `/api/cash-payments/{id}/reject`
- **Current access:** ADMIN (Protected by `SecurityConfig`)
- **Expected access:** ADMIN
- **Ownership protection:** N/A
- **Business validation:** Validates request is `PENDING`.
- **Risk:** Service layer does **not** check for `Role.ADMIN`.
- **Recommended change:** Add explicit `Role.ADMIN` check in `rejectRequest`.
- **Priority:** HIGH

---

## 6. Payment Allocation

### Controller: PaymentAllocationController
- **HTTP method:** POST
- **Path:** `/api/payment-allocations`
- **Current access:** ADMIN (Protected by `SecurityConfig`)
- **Expected access:** ADMIN
- **Ownership protection:** N/A
- **Business validation:** Validates `Payment` is `VERIFIED`. Ensures `Payment` and `Maintenance` belong to the exact same room (preventing cross-room allocations). Prevents over-allocation by checking remaining payment amount and remaining maintenance amount. Updates `MaintenanceStatus` accordingly.
- **Risk:** Service layer `createAllocation` does **not** verify `Role.ADMIN`. If `SecurityConfig` is bypassed, users could manually allocate their own payments.
- **Recommended change:** Add explicit `Role.ADMIN` check in `createAllocation`.
- **Priority:** HIGH

### Controller: PaymentAllocationController
- **HTTP method:** GET
- **Path:** `/api/payment-allocations/payment/{paymentId}`
- **Current access:** Authenticated
- **Expected access:** ADMIN or Room Owner
- **Ownership protection:** **NONE.** `PaymentAllocationService.getAllocationsByPayment` fetches the payment using `paymentRepository.findById(paymentId)`. It completely bypasses the ownership checks present in `PaymentService.getPaymentById`. 
- **Business validation:** Checks if payment exists.
- **Risk:** Broken Object Level Authorization (BOLA/IDOR). Any authenticated user can view the allocations of any payment in the system by providing a valid `paymentId`.
- **Recommended change:** Change `paymentRepository.findById(paymentId)` to `paymentService.getPaymentById(paymentId)` to inherit its strong ownership and role checks.
- **Priority:** **CRITICAL**

### Controller: PaymentAllocationController
- **HTTP method:** GET
- **Path:** `/api/payment-allocations/maintenance/{maintenanceId}`
- **Current access:** Authenticated
- **Expected access:** ADMIN or Room Owner
- **Ownership protection:** **Secure.** `PaymentAllocationService.getAllocationsByMaintenance` correctly calls `maintenanceService.getMaintenanceById(maintenanceId)`, which strictly enforces ownership and roles.
- **Business validation:** Checks if maintenance exists.
- **Risk:** None.
- **Recommended change:** None.
- **Priority:** LOW

---

## Summary & Recommendations

### 1. CRITICAL issues
- **BOLA in User Retrieval:** `GET /api/users/{id}` has no ownership checks. Any user can query any other user's personal details.
- **BOLA in Allocation Retrieval:** `GET /api/payment-allocations/payment/{paymentId}` uses the repository directly instead of the service layer, completely bypassing the `ADMIN` / Room Owner security checks for payments.

### 2. HIGH issues
- **Lack of Defense-in-Depth for Financial Operations:** Several endpoints rely *entirely* on `SecurityConfig` to restrict access to `ADMIN`. While `SecurityConfig` is currently correct, best practices mandate that the Service Layer also explicitly checks `currentUser.getRole() == Role.ADMIN` before performing irreversible financial actions. Affected endpoints:
    - `PUT /api/payments/{id}/verify`
    - `POST /api/cash-payments/{id}/verify`
    - `POST /api/cash-payments/{id}/reject`
    - `POST /api/payment-allocations`

### 3. MEDIUM issues
- **Lack of Defense-in-Depth for Admin Queries:** Endpoints like `GET /api/cash-payments/admin/pending` and `GET /api/payments/room/{roomId}` rely solely on `SecurityConfig` for role enforcement.

### 4. LOW issues
- `GET /api/rooms/{roomNumber}` lacks ownership checks. If room details are purely public/structural (floor, block), this is fine. If it exposes PII, it should be restricted.

### 5. Endpoints that are already correctly secured
- `POST /api/auth/register`, `POST /api/auth/login`
- `POST /api/maintenance`, `GET /api/maintenance/{id}`, `GET /api/maintenance/room/...`, `GET /api/maintenance/my`
- `POST /api/payments`, `GET /api/payments/my`, `GET /api/payments/{id}`, `PATCH /api/payments/{id}/proof`
- `POST /api/cash-payments`, `GET /api/cash-payments/{id}`, `GET /api/cash-payments/room/{roomId}`
- `GET /api/payment-allocations/maintenance/{maintenanceId}`

### 6. Temporary endpoints that should eventually be removed
- `GET /api/payments/room/{roomId}` (Originally built before the JWT `/my` context was fully implemented. Marked as temporary in code).

### 7. Recommended implementation order
1. **Fix CRITICAL BOLA vulnerabilities:** Update `UserService.getUserById` and `PaymentAllocationService.getAllocationsByPayment` to enforce ownership.
2. **Implement HIGH priority defense-in-depth:** Add `if (currentUser.getRole() != Role.ADMIN) throw new AccessDeniedException(...)` to all state-changing financial service methods.
3. **Address MEDIUM priority defense-in-depth:** Add similar `ADMIN` checks to admin-only retrieval service methods.
4. **Remove deprecated code:** Delete the temporary `GET /api/payments/room/{roomId}` endpoint and its related service method.
