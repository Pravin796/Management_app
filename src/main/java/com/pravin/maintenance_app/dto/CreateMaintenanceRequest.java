package com.pravin.maintenance_app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.YearMonth;

@Data
public class CreateMaintenanceRequest {

    @NotNull(message = "Room id is required")
    private Long roomId;

    @NotNull(message = "Billing month is required")
    private YearMonth billingMonth;
}