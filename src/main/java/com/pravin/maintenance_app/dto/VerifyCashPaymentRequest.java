package com.pravin.maintenance_app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class VerifyCashPaymentRequest {

    private String adminNote;

    @NotEmpty(message = "At least one payment allocation is required")
    @Valid
    private List<CashPaymentAllocationRequest> allocations;
}