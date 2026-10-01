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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.pravin.maintenance_app.dto.ApiErrorResponse;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Payments", description = "Endpoints for managing payments")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @Operation(summary = "Create a payment", description = "Creates a new payment. The backend determines the room from the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Payment created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or room already has a pending payment", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        PaymentResponse response = paymentMapper.toResponse(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get my payments", description = "Returns payments belonging to the authenticated user's room.")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments() {

        List<PaymentResponse> responses = paymentService
                .getMyPayments()
                .stream()
                .map(paymentMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get payment by ID", description = "Retrieves a payment by its ID. Users can only access payments for their own room.")
    @ApiResponse(responseCode = "200", description = "Payment returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "User is not allowed to access this payment", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        PaymentResponse response = paymentMapper.toResponse(payment);
        return ResponseEntity.ok(response);
    }

    // Temporary ID-based alternative for /my since Spring Security is not yet
    // implemented
    @Operation(summary = "Get payments by room", description = "Retrieves payments for a specific room. (Temporary ID-based alternative)")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByRoom(@PathVariable Long roomId) {
        List<Payment> payments = paymentService.getPaymentsByRoom(roomId);
        List<PaymentResponse> responses = payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Verify payment (ADMIN)", description = "Verifies a payment. This endpoint requires an authenticated user with ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Verified successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PutMapping("/{id}/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @PathVariable Long id) {

        Payment payment = paymentService.verifyPayment(id);
        PaymentResponse response = paymentMapper.toResponse(payment);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update payment proof", description = "Updates the proof for an existing payment")
    @ApiResponse(responseCode = "200", description = "Updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "User is not allowed to access this payment", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PatchMapping("/{id}/proof")
    public ResponseEntity<PaymentResponse> updatePaymentProof(
            @PathVariable Long id,
            @Valid @RequestBody com.pravin.maintenance_app.dto.UpdatePaymentProofRequest request) {
        Payment payment = paymentService.updatePaymentProof(id, request);
        PaymentResponse response = paymentMapper.toResponse(payment);
        return ResponseEntity.ok(response);
    }

}
