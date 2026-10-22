package com.pravin.maintenance_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private Long totalRooms;
    private BigDecimal totalMaintenanceCollected;
    private BigDecimal totalOutstandingBalance;
    private Long pendingOnlinePayments;
    private Long pendingCashRequests;
    private Long verifiedCashPayments;
}
