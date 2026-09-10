package com.pravin.maintenance_app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentAllocationRequest {

    @NotNull(message = "Payment id is required")
    private Long paymentId;

    @NotNull(message = "Maintenance id is required")
    private Long maintenanceId;

    @NotNull(message = "Allocation amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Allocation amount must be greater than zero"
    )
    private BigDecimal amount;
}