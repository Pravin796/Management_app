package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.CreatePaymentAllocationRequest;
import com.pravin.maintenance_app.dto.PaymentAllocationResponse;
import com.pravin.maintenance_app.entity.PaymentAllocation;
import com.pravin.maintenance_app.mapper.PaymentAllocationMapper;
import com.pravin.maintenance_app.service.PaymentAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.pravin.maintenance_app.dto.ApiErrorResponse;

import java.util.List;

@Tag(name = "Payment Allocations", description = "Endpoints for payment allocations")
@RestController
@RequestMapping("/api/payment-allocations")
@RequiredArgsConstructor
public class PaymentAllocationController {

    private final PaymentAllocationService paymentAllocationService;
    private final PaymentAllocationMapper paymentAllocationMapper;

    @Operation(summary = "Create payment allocation", description = "Allocates a payment to a maintenance record")
    @ApiResponse(responseCode = "201", description = "Created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping
    public ResponseEntity<PaymentAllocationResponse> createAllocation(
            @Valid @RequestBody CreatePaymentAllocationRequest request
    ) {

        PaymentAllocation allocation =
                paymentAllocationService.createAllocation(request);

        PaymentAllocationResponse response =
                paymentAllocationMapper.toResponse(allocation);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get allocations by payment", description = "Retrieves all allocations for a specific payment")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<PaymentAllocationResponse>>
    getAllocationsByPayment(
            @PathVariable Long paymentId
    ) {

        List<PaymentAllocationResponse> responses =
                paymentAllocationService
                        .getAllocationsByPayment(paymentId)
                        .stream()
                        .map(paymentAllocationMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get allocations by maintenance", description = "Retrieves all allocations for a specific maintenance record")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<List<PaymentAllocationResponse>>
    getAllocationsByMaintenance(
            @PathVariable Long maintenanceId
    ) {

        List<PaymentAllocationResponse> responses =
                paymentAllocationService
                        .getAllocationsByMaintenance(maintenanceId)
                        .stream()
                        .map(paymentAllocationMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }
}