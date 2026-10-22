package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.PaymentMethod;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.ENUM.UserStatus;
import com.pravin.maintenance_app.config.MaintenanceProperties;
import com.pravin.maintenance_app.dto.UserDashboardResponse;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.mapper.UserMapper;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import com.pravin.maintenance_app.repository.PaymentAllocationRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.repository.UserRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceDashboardTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private MaintenanceProperties maintenanceProperties;

    @InjectMocks
    private UserService userService;

    private User createResident(Long userId, String name, Long roomId, String roomNumber, boolean isExempt) {
        Room room = new Room();
        room.setId(roomId);
        room.setRoomNumber(roomNumber);
        room.setMaintenanceExempt(isExempt);

        User user = new User();
        user.setId(userId);
        user.setName(name);
        user.setMobileNumber("98765432" + (roomId < 10 ? "0" + roomId : roomId));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoom(room);

        return user;
    }

    private Maintenance createMaintenance(Long id, Room room, YearMonth month, BigDecimal amount, MaintenanceStatus status) {
        Maintenance m = new Maintenance();
        m.setId(id);
        m.setRoom(room);
        m.setBillingMonth(month);
        m.setAmount(amount);
        m.setStatus(status);
        m.setCreatedAt(LocalDateTime.now().minusDays(10));
        m.setUpdatedAt(LocalDateTime.now().minusDays(10));
        return m;
    }

    private Payment createPayment(Long id, Room room, BigDecimal amount, PaymentMethod method, PaymentStatus status, LocalDateTime createdAt) {
        Payment p = new Payment();
        p.setId(id);
        p.setRoom(room);
        p.setAmount(amount);
        p.setMethod(method);
        p.setStatus(status);
        p.setTransactionReference("REF-" + id);
        p.setScreenshotUrl("https://cloudinary.com/proof-" + id + ".png");
        p.setScreenshotPublicId("proof-public-id-" + id);
        p.setCreatedAt(createdAt);
        p.setUpdatedAt(createdAt);
        return p;
    }

    @BeforeEach
    void setUp() {
        lenient().when(maintenanceProperties.getMonthlyAmount()).thenReturn(new BigDecimal("500.00"));
    }

    @Test
    void testAuthenticatedAccess_ReturnsDashboardSuccessfully() {
        User resident = createResident(1L, "Alice Johnson", 10L, "101", false);
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        YearMonth currentMonth = YearMonth.now();
        Maintenance m = createMaintenance(100L, resident.getRoom(), currentMonth, new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        when(maintenanceRepository.findByRoomId(10L)).thenReturn(List.of(m));
        when(paymentAllocationRepository.getVerifiedAmountByRoomIdGroupedByMaintenanceId(10L))
                .thenReturn(Collections.emptyList());

        LocalDateTime paymentTime = LocalDateTime.now().minusHours(2);
        Payment p = createPayment(50L, resident.getRoom(), new BigDecimal("500.00"), PaymentMethod.ONLINE, PaymentStatus.VERIFIED, paymentTime);
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(10L))
                .thenReturn(Optional.of(p));

        UserDashboardResponse dashboard = userService.getUserDashboard();

        assertNotNull(dashboard);
        assertEquals("Alice Johnson", dashboard.getResidentName());
        assertEquals("101", dashboard.getRoomNumber());
        assertEquals(new BigDecimal("500.00"), dashboard.getMaintenanceRate());
        assertEquals(new BigDecimal("500.00"), dashboard.getOutstandingBalance());

        assertNotNull(dashboard.getCurrentMaintenance());
        assertEquals(currentMonth, dashboard.getCurrentMaintenance().getBillingMonth());
        assertEquals(new BigDecimal("500.00"), dashboard.getCurrentMaintenance().getAmount());
        assertEquals(MaintenanceStatus.PENDING, dashboard.getCurrentMaintenance().getStatus());

        assertNotNull(dashboard.getRecentPayment());
        assertEquals(50L, dashboard.getRecentPayment().getPaymentId());
        assertEquals(new BigDecimal("500.00"), dashboard.getRecentPayment().getAmount());
        assertEquals(PaymentMethod.ONLINE, dashboard.getRecentPayment().getMethod());
        assertEquals(PaymentStatus.VERIFIED, dashboard.getRecentPayment().getStatus());
        assertEquals(paymentTime, dashboard.getRecentPayment().getCreatedAt());
    }

    @Test
    void testResidentDataIsolation_OnlyReturnsAuthenticatedResidentData() {
        User residentA = createResident(1L, "Resident A", 10L, "101", false);
        User residentB = createResident(2L, "Resident B", 20L, "202", false);

        when(currentUserService.getCurrentUser()).thenReturn(residentA);

        Maintenance mA = createMaintenance(100L, residentA.getRoom(), YearMonth.now(), new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        when(maintenanceRepository.findByRoomId(10L)).thenReturn(List.of(mA));
        when(paymentAllocationRepository.getVerifiedAmountByRoomIdGroupedByMaintenanceId(10L)).thenReturn(Collections.emptyList());
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(10L)).thenReturn(Optional.empty());

        UserDashboardResponse dashboardA = userService.getUserDashboard();

        assertEquals("Resident A", dashboardA.getResidentName());
        assertEquals("101", dashboardA.getRoomNumber());

        verify(maintenanceRepository).findByRoomId(10L);
        verify(maintenanceRepository, never()).findByRoomId(20L);
        verify(paymentRepository).findFirstByRoomIdOrderByCreatedAtDescIdDesc(10L);
        verify(paymentRepository, never()).findFirstByRoomIdOrderByCreatedAtDescIdDesc(20L);

        // Switch to resident B
        reset(maintenanceRepository, paymentRepository, paymentAllocationRepository);
        when(currentUserService.getCurrentUser()).thenReturn(residentB);

        Maintenance mB = createMaintenance(200L, residentB.getRoom(), YearMonth.now(), new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        when(maintenanceRepository.findByRoomId(20L)).thenReturn(List.of(mB));
        when(paymentAllocationRepository.getVerifiedAmountByRoomIdGroupedByMaintenanceId(20L)).thenReturn(Collections.emptyList());
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(20L)).thenReturn(Optional.empty());

        UserDashboardResponse dashboardB = userService.getUserDashboard();

        assertEquals("Resident B", dashboardB.getResidentName());
        assertEquals("202", dashboardB.getRoomNumber());

        verify(maintenanceRepository).findByRoomId(20L);
        verify(maintenanceRepository, never()).findByRoomId(10L);
        verify(paymentRepository).findFirstByRoomIdOrderByCreatedAtDescIdDesc(20L);
        verify(paymentRepository, never()).findFirstByRoomIdOrderByCreatedAtDescIdDesc(10L);
    }

    @Test
    void testUnpaidMaintenance_AccumulatesOutstandingBalance() {
        User resident = createResident(1L, "Bob", 15L, "305", false);
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        Maintenance m1 = createMaintenance(101L, resident.getRoom(), YearMonth.of(2026, 7), new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        Maintenance m2 = createMaintenance(102L, resident.getRoom(), YearMonth.of(2026, 8), new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        Maintenance m3 = createMaintenance(103L, resident.getRoom(), YearMonth.of(2026, 9), new BigDecimal("500.00"), MaintenanceStatus.PENDING);

        when(maintenanceRepository.findByRoomId(15L)).thenReturn(List.of(m1, m2, m3));
        when(paymentAllocationRepository.getVerifiedAmountByRoomIdGroupedByMaintenanceId(15L)).thenReturn(Collections.emptyList());
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(15L)).thenReturn(Optional.empty());

        UserDashboardResponse dashboard = userService.getUserDashboard();

        assertEquals(new BigDecimal("1500.00"), dashboard.getOutstandingBalance());
    }

    @Test
    void testPartialAllocations_ReducesOutstandingBalanceCorrectly() {
        User resident = createResident(1L, "Charlie", 16L, "306", false);
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        Maintenance m1 = createMaintenance(201L, resident.getRoom(), YearMonth.of(2026, 8), new BigDecimal("500.00"), MaintenanceStatus.PARTIALLY_PAID);
        Maintenance m2 = createMaintenance(202L, resident.getRoom(), YearMonth.of(2026, 9), new BigDecimal("500.00"), MaintenanceStatus.PENDING);

        when(maintenanceRepository.findByRoomId(16L)).thenReturn(List.of(m1, m2));

        // ₹300 verified allocation for m1
        List<Object[]> allocations = new ArrayList<>();
        allocations.add(new Object[]{201L, new BigDecimal("300.00")});
        when(paymentAllocationRepository.getVerifiedAmountByRoomIdGroupedByMaintenanceId(16L)).thenReturn(allocations);
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(16L)).thenReturn(Optional.empty());

        UserDashboardResponse dashboard = userService.getUserDashboard();

        // m1: 500 - 300 = 200, m2: 500 - 0 = 500 => Total = 700.00
        assertEquals(new BigDecimal("700.00"), dashboard.getOutstandingBalance());
    }

    @Test
    void testPendingAndRejectedPayments_DoNotReduceOutstandingBalance() {
        User resident = createResident(1L, "David", 17L, "307", false);
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        Maintenance m = createMaintenance(301L, resident.getRoom(), YearMonth.now(), new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        when(maintenanceRepository.findByRoomId(17L)).thenReturn(List.of(m));

        // Allocation query only returns verified payment allocations; pending/rejected payments yield 0 verified allocations
        when(paymentAllocationRepository.getVerifiedAmountByRoomIdGroupedByMaintenanceId(17L)).thenReturn(Collections.emptyList());

        // A pending payment exists in payment repository
        Payment pendingPayment = createPayment(77L, resident.getRoom(), new BigDecimal("500.00"), PaymentMethod.ONLINE, PaymentStatus.PENDING, LocalDateTime.now());
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(17L)).thenReturn(Optional.of(pendingPayment));

        UserDashboardResponse dashboard = userService.getUserDashboard();

        // Outstanding balance remains full ₹500.00 because pending payment does not count as paid
        assertEquals(new BigDecimal("500.00"), dashboard.getOutstandingBalance());
        assertNotNull(dashboard.getRecentPayment());
        assertEquals(PaymentStatus.PENDING, dashboard.getRecentPayment().getStatus());
    }

    @Test
    void testExemptRooms_ReturnsZeroOutstandingAndNullCurrentMaintenance() {
        User exemptResident = createResident(1L, "Exempt Officer", 18L, "001", true);
        when(currentUserService.getCurrentUser()).thenReturn(exemptResident);

        UserDashboardResponse dashboard = userService.getUserDashboard();

        assertEquals(new BigDecimal("500.00"), dashboard.getMaintenanceRate());
        assertEquals(new BigDecimal("0.00"), dashboard.getOutstandingBalance());
        assertNull(dashboard.getCurrentMaintenance());

        // Verify maintenance repository is not even queried for exempt room
        verify(maintenanceRepository, never()).findByRoomId(anyLong());
        verify(paymentAllocationRepository, never()).getVerifiedAmountByRoomIdGroupedByMaintenanceId(anyLong());
    }

    @Test
    void testNoPaymentHistory_ReturnsNullRecentPayment() {
        User resident = createResident(1L, "Eve", 19L, "401", false);
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        when(maintenanceRepository.findByRoomId(19L)).thenReturn(Collections.emptyList());
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(19L)).thenReturn(Optional.empty());

        UserDashboardResponse dashboard = userService.getUserDashboard();

        assertNull(dashboard.getRecentPayment());
        assertEquals(new BigDecimal("0.00"), dashboard.getOutstandingBalance());
        assertNull(dashboard.getCurrentMaintenance());
    }

    @Test
    void testMissingCurrentMonthRecord_ReturnsNullAndDoesNotCreateRecord() {
        User resident = createResident(1L, "Frank", 21L, "402", false);
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        YearMonth pastMonth = YearMonth.now().minusMonths(1);
        Maintenance pastMaintenance = createMaintenance(401L, resident.getRoom(), pastMonth, new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        when(maintenanceRepository.findByRoomId(21L)).thenReturn(List.of(pastMaintenance));
        when(paymentAllocationRepository.getVerifiedAmountByRoomIdGroupedByMaintenanceId(21L)).thenReturn(Collections.emptyList());
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(21L)).thenReturn(Optional.empty());

        UserDashboardResponse dashboard = userService.getUserDashboard();

        assertNull(dashboard.getCurrentMaintenance());
        assertEquals(new BigDecimal("500.00"), dashboard.getOutstandingBalance());

        // Ensure no new maintenance was created as a side effect
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }

    @Test
    void testPaidMaintenance_IsExcludedFromOutstandingBalance() {
        User resident = createResident(1L, "Grace", 22L, "403", false);
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        Maintenance mPaid = createMaintenance(501L, resident.getRoom(), YearMonth.of(2026, 8), new BigDecimal("500.00"), MaintenanceStatus.PAID);
        Maintenance mPending = createMaintenance(502L, resident.getRoom(), YearMonth.of(2026, 9), new BigDecimal("500.00"), MaintenanceStatus.PENDING);

        when(maintenanceRepository.findByRoomId(22L)).thenReturn(List.of(mPaid, mPending));

        List<Object[]> allocations = new ArrayList<>();
        allocations.add(new Object[]{501L, new BigDecimal("500.00")});
        when(paymentAllocationRepository.getVerifiedAmountByRoomIdGroupedByMaintenanceId(22L)).thenReturn(allocations);
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(22L)).thenReturn(Optional.empty());

        UserDashboardResponse dashboard = userService.getUserDashboard();

        // Only mPending contributes to outstanding balance
        assertEquals(new BigDecimal("500.00"), dashboard.getOutstandingBalance());
    }

    @Test
    void testRecentPayment_DoesNotExposeSensitiveProofDetails() {
        User resident = createResident(1L, "Hannah", 23L, "404", false);
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        LocalDateTime now = LocalDateTime.now();
        Payment paymentWithSensitiveData = createPayment(99L, resident.getRoom(), new BigDecimal("500.00"),
                PaymentMethod.ONLINE, PaymentStatus.VERIFIED, now);
        paymentWithSensitiveData.setTransactionReference("CONFIDENTIAL_UTR_12345");
        paymentWithSensitiveData.setScreenshotUrl("https://cloudinary.com/secret-proof.jpg");
        paymentWithSensitiveData.setScreenshotPublicId("secret-proof-id");

        when(maintenanceRepository.findByRoomId(23L)).thenReturn(Collections.emptyList());
        when(paymentRepository.findFirstByRoomIdOrderByCreatedAtDescIdDesc(23L)).thenReturn(Optional.of(paymentWithSensitiveData));

        UserDashboardResponse dashboard = userService.getUserDashboard();

        assertNotNull(dashboard.getRecentPayment());
        assertEquals(99L, dashboard.getRecentPayment().getPaymentId());
        assertEquals(new BigDecimal("500.00"), dashboard.getRecentPayment().getAmount());
        assertEquals(PaymentMethod.ONLINE, dashboard.getRecentPayment().getMethod());
        assertEquals(PaymentStatus.VERIFIED, dashboard.getRecentPayment().getStatus());
        assertEquals(now, dashboard.getRecentPayment().getCreatedAt());

        // RecentPaymentDto class has no getter methods for sensitive fields
        assertFalse(hasMethod(dashboard.getRecentPayment().getClass(), "getTransactionReference"));
        assertFalse(hasMethod(dashboard.getRecentPayment().getClass(), "getScreenshotUrl"));
        assertFalse(hasMethod(dashboard.getRecentPayment().getClass(), "getScreenshotPublicId"));
    }

    @Test
    void testUserWithoutRoom_ThrowsBusinessValidationException() {
        User residentWithoutRoom = new User();
        residentWithoutRoom.setId(99L);
        residentWithoutRoom.setName("Homeless User");
        residentWithoutRoom.setRoom(null);

        when(currentUserService.getCurrentUser()).thenReturn(residentWithoutRoom);

        assertThrows(BusinessValidationException.class, () -> userService.getUserDashboard());
    }

    private boolean hasMethod(Class<?> clazz, String methodName) {
        try {
            clazz.getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
