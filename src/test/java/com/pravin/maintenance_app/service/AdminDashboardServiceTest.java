package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.CashPaymentRequestStatus;
import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.PaymentMethod;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.ENUM.RoomStatus;
import com.pravin.maintenance_app.ENUM.UserStatus;
import com.pravin.maintenance_app.dto.AdminDashboardResponse;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.repository.CashPaymentRequestRepository;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import com.pravin.maintenance_app.repository.PaymentAllocationRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.repository.RoomRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminDashboardServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private CashPaymentRequestRepository cashPaymentRequestRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    private User adminUser;
    private User residentUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setName("Admin User");
        adminUser.setRole(Role.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        residentUser = new User();
        residentUser.setId(2L);
        residentUser.setName("Resident User");
        residentUser.setRole(Role.USER);
        residentUser.setStatus(UserStatus.ACTIVE);
    }

    private Room createRoom(Long id, String roomNumber, boolean isExempt) {
        Room room = new Room();
        room.setId(id);
        room.setRoomNumber(roomNumber);
        room.setStatus(RoomStatus.ACTIVE);
        room.setMaintenanceExempt(isExempt);
        return room;
    }

    private Maintenance createMaintenance(Long id, Room room, YearMonth month, BigDecimal amount, MaintenanceStatus status) {
        Maintenance m = new Maintenance();
        m.setId(id);
        m.setRoom(room);
        m.setBillingMonth(month);
        m.setAmount(amount);
        m.setStatus(status);
        m.setCreatedAt(LocalDateTime.now().minusDays(5));
        m.setUpdatedAt(LocalDateTime.now().minusDays(5));
        return m;
    }

    @Test
    void testResidentForbidden_ThrowsAccessDeniedException() {
        when(currentUserService.getCurrentUser()).thenReturn(residentUser);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> adminDashboardService.getAdminDashboard()
        );

        assertEquals("Only ADMIN can access the admin dashboard", exception.getMessage());
        verifyNoInteractions(roomRepository, maintenanceRepository, paymentRepository, paymentAllocationRepository, cashPaymentRequestRepository);
    }

    @Test
    void testEmptyData_ReturnsZeroCountsAndZeroBalances() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(roomRepository.count()).thenReturn(0L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.ONLINE, PaymentStatus.PENDING)).thenReturn(0L);
        when(cashPaymentRequestRepository.countByStatus(CashPaymentRequestStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.CASH, PaymentStatus.VERIFIED)).thenReturn(0L);
        when(maintenanceRepository.findAllNonExemptMaintenance()).thenReturn(Collections.emptyList());
        when(paymentAllocationRepository.getVerifiedAmountGroupedByMaintenanceId()).thenReturn(Collections.emptyList());

        AdminDashboardResponse response = adminDashboardService.getAdminDashboard();

        assertNotNull(response);
        assertEquals(0L, response.getTotalRooms());
        assertEquals(new BigDecimal("0.00"), response.getTotalMaintenanceCollected());
        assertEquals(new BigDecimal("0.00"), response.getTotalOutstandingBalance());
        assertEquals(0L, response.getPendingOnlinePayments());
        assertEquals(0L, response.getPendingCashRequests());
        assertEquals(0L, response.getVerifiedCashPayments());
    }

    @Test
    void testAdminAccess_ReturnsCorrectAggregatedMetrics() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(roomRepository.count()).thenReturn(10L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.ONLINE, PaymentStatus.PENDING)).thenReturn(2L);
        when(cashPaymentRequestRepository.countByStatus(CashPaymentRequestStatus.PENDING)).thenReturn(1L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.CASH, PaymentStatus.VERIFIED)).thenReturn(4L);

        Room room1 = createRoom(1L, "101", false);
        Room room2 = createRoom(2L, "102", false);

        Maintenance m1 = createMaintenance(101L, room1, YearMonth.of(2026, 9), new BigDecimal("500.00"), MaintenanceStatus.PAID);
        Maintenance m2 = createMaintenance(102L, room2, YearMonth.of(2026, 9), new BigDecimal("500.00"), MaintenanceStatus.PENDING);

        when(maintenanceRepository.findAllNonExemptMaintenance()).thenReturn(List.of(m1, m2));

        List<Object[]> allocations = new ArrayList<>();
        allocations.add(new Object[]{101L, new BigDecimal("500.00")});
        when(paymentAllocationRepository.getVerifiedAmountGroupedByMaintenanceId()).thenReturn(allocations);

        AdminDashboardResponse response = adminDashboardService.getAdminDashboard();

        assertNotNull(response);
        assertEquals(10L, response.getTotalRooms());
        assertEquals(new BigDecimal("500.00"), response.getTotalMaintenanceCollected());
        assertEquals(new BigDecimal("500.00"), response.getTotalOutstandingBalance());
        assertEquals(2L, response.getPendingOnlinePayments());
        assertEquals(1L, response.getPendingCashRequests());
        assertEquals(4L, response.getVerifiedCashPayments());
    }

    @Test
    void testPendingPayments_NotCountedAsVerifiedCollections() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(roomRepository.count()).thenReturn(5L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.ONLINE, PaymentStatus.PENDING)).thenReturn(3L);
        when(cashPaymentRequestRepository.countByStatus(CashPaymentRequestStatus.PENDING)).thenReturn(2L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.CASH, PaymentStatus.VERIFIED)).thenReturn(0L);

        Room room = createRoom(1L, "101", false);
        Maintenance m = createMaintenance(201L, room, YearMonth.of(2026, 10), new BigDecimal("500.00"), MaintenanceStatus.PENDING);

        when(maintenanceRepository.findAllNonExemptMaintenance()).thenReturn(List.of(m));
        // Allocation repository only returns allocations for VERIFIED payments, so pending payments yield empty list
        when(paymentAllocationRepository.getVerifiedAmountGroupedByMaintenanceId()).thenReturn(Collections.emptyList());

        AdminDashboardResponse response = adminDashboardService.getAdminDashboard();

        assertEquals(3L, response.getPendingOnlinePayments());
        assertEquals(2L, response.getPendingCashRequests());
        assertEquals(0L, response.getVerifiedCashPayments());
        // Pending payments must not count towards collected maintenance
        assertEquals(new BigDecimal("0.00"), response.getTotalMaintenanceCollected());
        // Full maintenance remains outstanding
        assertEquals(new BigDecimal("500.00"), response.getTotalOutstandingBalance());
    }

    @Test
    void testPartialAllocations_ReducesOutstandingBalanceAndCountsCollected() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(roomRepository.count()).thenReturn(4L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.ONLINE, PaymentStatus.PENDING)).thenReturn(0L);
        when(cashPaymentRequestRepository.countByStatus(CashPaymentRequestStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.CASH, PaymentStatus.VERIFIED)).thenReturn(1L);

        Room room = createRoom(1L, "201", false);
        // Maintenance of 600 with partial payment allocation of 250
        Maintenance m = createMaintenance(301L, room, YearMonth.of(2026, 10), new BigDecimal("600.00"), MaintenanceStatus.PARTIALLY_PAID);

        when(maintenanceRepository.findAllNonExemptMaintenance()).thenReturn(List.of(m));

        List<Object[]> allocations = new ArrayList<>();
        allocations.add(new Object[]{301L, new BigDecimal("250.00")});
        when(paymentAllocationRepository.getVerifiedAmountGroupedByMaintenanceId()).thenReturn(allocations);

        AdminDashboardResponse response = adminDashboardService.getAdminDashboard();

        // Collected = 250.00, Outstanding = 600 - 250 = 350.00
        assertEquals(new BigDecimal("250.00"), response.getTotalMaintenanceCollected());
        assertEquals(new BigDecimal("350.00"), response.getTotalOutstandingBalance());
    }

    @Test
    void testOutstandingBalances_AccumulatesMultipleUnpaidRecords() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(roomRepository.count()).thenReturn(6L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.ONLINE, PaymentStatus.PENDING)).thenReturn(0L);
        when(cashPaymentRequestRepository.countByStatus(CashPaymentRequestStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.CASH, PaymentStatus.VERIFIED)).thenReturn(0L);

        Room room1 = createRoom(1L, "101", false);
        Room room2 = createRoom(2L, "102", false);

        Maintenance m1 = createMaintenance(401L, room1, YearMonth.of(2026, 8), new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        Maintenance m2 = createMaintenance(402L, room1, YearMonth.of(2026, 9), new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        Maintenance m3 = createMaintenance(403L, room2, YearMonth.of(2026, 9), new BigDecimal("750.00"), MaintenanceStatus.PENDING);

        when(maintenanceRepository.findAllNonExemptMaintenance()).thenReturn(List.of(m1, m2, m3));
        when(paymentAllocationRepository.getVerifiedAmountGroupedByMaintenanceId()).thenReturn(Collections.emptyList());

        AdminDashboardResponse response = adminDashboardService.getAdminDashboard();

        // 500 + 500 + 750 = 1750.00
        assertEquals(new BigDecimal("1750.00"), response.getTotalOutstandingBalance());
        assertEquals(new BigDecimal("0.00"), response.getTotalMaintenanceCollected());
    }

    @Test
    void testExemptRooms_NotCountedInMaintenanceOrBalances_CountedInTotalRooms() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        // Total rooms includes exempt rooms
        when(roomRepository.count()).thenReturn(8L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.ONLINE, PaymentStatus.PENDING)).thenReturn(0L);
        when(cashPaymentRequestRepository.countByStatus(CashPaymentRequestStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.CASH, PaymentStatus.VERIFIED)).thenReturn(0L);

        Room nonExemptRoom = createRoom(1L, "101", false);
        Room exemptRoom = createRoom(2L, "001", true);

        Maintenance mNonExempt = createMaintenance(501L, nonExemptRoom, YearMonth.of(2026, 10), new BigDecimal("500.00"), MaintenanceStatus.PENDING);
        Maintenance mExempt = createMaintenance(502L, exemptRoom, YearMonth.of(2026, 10), new BigDecimal("500.00"), MaintenanceStatus.PENDING);

        // Even if an exempt maintenance record was returned, service skips it
        when(maintenanceRepository.findAllNonExemptMaintenance()).thenReturn(List.of(mNonExempt, mExempt));
        when(paymentAllocationRepository.getVerifiedAmountGroupedByMaintenanceId()).thenReturn(Collections.emptyList());

        AdminDashboardResponse response = adminDashboardService.getAdminDashboard();

        assertEquals(8L, response.getTotalRooms());
        // Only mNonExempt is counted towards outstanding balance
        assertEquals(new BigDecimal("500.00"), response.getTotalOutstandingBalance());
        assertEquals(new BigDecimal("0.00"), response.getTotalMaintenanceCollected());
    }

    @Test
    void testDirectPaidMaintenance_CountedAsCollectedWithoutAllocationRecords() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(roomRepository.count()).thenReturn(3L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.ONLINE, PaymentStatus.PENDING)).thenReturn(0L);
        when(cashPaymentRequestRepository.countByStatus(CashPaymentRequestStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.countByMethodAndStatus(PaymentMethod.CASH, PaymentStatus.VERIFIED)).thenReturn(1L);

        Room room = createRoom(1L, "301", false);
        Maintenance m = createMaintenance(601L, room, YearMonth.of(2026, 10), new BigDecimal("500.00"), MaintenanceStatus.PAID);

        when(maintenanceRepository.findAllNonExemptMaintenance()).thenReturn(List.of(m));
        // No explicit allocation row exists, but record is marked PAID
        when(paymentAllocationRepository.getVerifiedAmountGroupedByMaintenanceId()).thenReturn(Collections.emptyList());

        AdminDashboardResponse response = adminDashboardService.getAdminDashboard();

        assertEquals(new BigDecimal("500.00"), response.getTotalMaintenanceCollected());
        assertEquals(new BigDecimal("0.00"), response.getTotalOutstandingBalance());
    }
}
