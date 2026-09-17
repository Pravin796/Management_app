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

import java.util.List;

@RestController
@RequestMapping("/api/payment-allocations")
@RequiredArgsConstructor
public class PaymentAllocationController {

    private final PaymentAllocationService paymentAllocationService;
    private final PaymentAllocationMapper paymentAllocationMapper;

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