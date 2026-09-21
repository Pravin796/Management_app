package com.pravin.maintenance_app.dto;

import com.pravin.maintenance_app.ENUM.CashPaymentRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashPaymentRequestResponse {
    private Long id;
    private String roomNumber;
    private BigDecimal amount;
    private CashPaymentRequestStatus status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime rejectedAt;
}
