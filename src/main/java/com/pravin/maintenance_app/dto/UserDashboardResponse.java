package com.pravin.maintenance_app.dto;

import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.PaymentMethod;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardResponse {

    private String residentName;
    private String roomNumber;
    private BigDecimal maintenanceRate;
    private BigDecimal outstandingBalance;
    private CurrentMaintenanceDto currentMaintenance;
    private RecentPaymentDto recentPayment;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentMaintenanceDto {
        private YearMonth billingMonth;
        private BigDecimal amount;
        private MaintenanceStatus status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentPaymentDto {
        private Long paymentId;
        private BigDecimal amount;
        private PaymentMethod method;
        private PaymentStatus status;
        private LocalDateTime createdAt;
    }
}
