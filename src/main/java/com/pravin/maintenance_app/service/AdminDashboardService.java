package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.CashPaymentRequestStatus;
import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.PaymentMethod;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.dto.AdminDashboardResponse;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.repository.CashPaymentRequestRepository;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import com.pravin.maintenance_app.repository.PaymentAllocationRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.repository.RoomRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final RoomRepository roomRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final CashPaymentRequestRepository cashPaymentRequestRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can access the admin dashboard");
        }

        Long totalRooms = roomRepository.count();

        Long pendingOnlinePayments = paymentRepository.countByMethodAndStatus(
                PaymentMethod.ONLINE,
                PaymentStatus.PENDING
        );

        Long pendingCashRequests = cashPaymentRequestRepository.countByStatus(
                CashPaymentRequestStatus.PENDING
        );

        Long verifiedCashPayments = paymentRepository.countByMethodAndStatus(
                PaymentMethod.CASH,
                PaymentStatus.VERIFIED
        );

        List<Maintenance> nonExemptMaintenanceList = maintenanceRepository.findAllNonExemptMaintenance();

        Map<Long, BigDecimal> allocatedMap = new HashMap<>();
        List<Object[]> allocationRows = paymentAllocationRepository.getVerifiedAmountGroupedByMaintenanceId();
        if (allocationRows != null && !allocationRows.isEmpty()) {
            for (Object[] row : allocationRows) {
                Long maintenanceId = (Long) row[0];
                BigDecimal allocatedAmount = row[1] instanceof BigDecimal b ? b : new BigDecimal(row[1].toString());
                allocatedMap.put(maintenanceId, allocatedAmount);
            }
        }

        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;

        if (nonExemptMaintenanceList != null) {
            for (Maintenance m : nonExemptMaintenanceList) {
                if (m.getRoom() != null && m.getRoom().isMaintenanceExempt()) {
                    continue;
                }

                BigDecimal allocated = allocatedMap.getOrDefault(m.getId(), BigDecimal.ZERO);

                if (m.getStatus() == MaintenanceStatus.PAID && allocated.compareTo(BigDecimal.ZERO) == 0) {
                    totalCollected = totalCollected.add(m.getAmount());
                } else {
                    totalCollected = totalCollected.add(allocated.min(m.getAmount()));
                }

                if (m.getStatus() != MaintenanceStatus.PAID) {
                    BigDecimal remaining = m.getAmount().subtract(allocated);
                    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                        totalOutstanding = totalOutstanding.add(remaining);
                    }
                }
            }
        }

        BigDecimal totalMaintenanceCollected = totalCollected.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalOutstandingBalance = totalOutstanding.setScale(2, RoundingMode.HALF_UP);

        return new AdminDashboardResponse(
                totalRooms,
                totalMaintenanceCollected,
                totalOutstandingBalance,
                pendingOnlinePayments,
                pendingCashRequests,
                verifiedCashPayments
        );
    }
}
