package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.CreatePaymentRequest;
import com.pravin.maintenance_app.dto.PaymentResponse;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.mapper.PaymentMapper;
import com.pravin.maintenance_app.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        PaymentResponse response = paymentMapper.toResponse(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        PaymentResponse response = paymentMapper.toResponse(payment);
        return ResponseEntity.ok(response);
    }

    // Temporary ID-based alternative for /my since Spring Security is not yet implemented
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByRoom(@PathVariable Long roomId) {
        List<Payment> payments = paymentService.getPaymentsByRoom(roomId);
        List<PaymentResponse> responses = payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @PathVariable Long id
    ) {

        Payment payment = paymentService.verifyPayment(id);
        PaymentResponse response = paymentMapper.toResponse(payment);

        return ResponseEntity.ok(response);
    }
}
