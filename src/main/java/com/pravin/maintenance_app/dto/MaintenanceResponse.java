package com.pravin.maintenance_app.dto;

import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceResponse {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private YearMonth billingMonth;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private BigDecimal totalAmount;
    private BigDecimal remainingBalance;
    private MaintenanceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal getTotalAmount() {
        return totalAmount != null ? totalAmount : amount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance != null ? remainingBalance : (remainingAmount != null ? remainingAmount : amount);
    }
}
