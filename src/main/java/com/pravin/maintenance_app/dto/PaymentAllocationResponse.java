package com.pravin.maintenance_app.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentAllocationResponse {

    private Long id;
    private Long paymentId;
    private Long maintenanceId;
    private BigDecimal amount;
}