package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.CreateCashPaymentRequest;
import com.pravin.maintenance_app.dto.CashPaymentRequestResponse;
import com.pravin.maintenance_app.dto.VerifyCashPaymentRequest;
import com.pravin.maintenance_app.entity.CashPaymentRequest;
import com.pravin.maintenance_app.mapper.CashPaymentRequestMapper;
import com.pravin.maintenance_app.service.CashPaymentRequestService;
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

@Tag(name = "Cash Payments", description = "Endpoints for cash payment requests")
@RestController
@RequestMapping("/api/cash-payments")
@RequiredArgsConstructor
public class CashPaymentRequestController {

    private final CashPaymentRequestService cashPaymentRequestService;
    private final CashPaymentRequestMapper cashPaymentRequestMapper;

    @Operation(summary = "Create cash payment request", description = "Creates a new cash payment request")
    @ApiResponse(responseCode = "201", description = "Created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping
    public ResponseEntity<CashPaymentRequestResponse> createRequest(
            @Valid @RequestBody CreateCashPaymentRequest request) {
        CashPaymentRequest cashPaymentRequest = cashPaymentRequestService.createRequest(request);
        CashPaymentRequestResponse response = cashPaymentRequestMapper.toResponse(cashPaymentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get cash request by ID", description = "Retrieves a cash payment request by its ID")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<CashPaymentRequestResponse> getRequestById(@PathVariable Long id) {
        CashPaymentRequest cashPaymentRequest = cashPaymentRequestService.getRequestById(id);
        CashPaymentRequestResponse response = cashPaymentRequestMapper.toResponse(cashPaymentRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get cash requests by room", description = "Retrieves all cash payment requests for a specific room")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<CashPaymentRequestResponse>> getRequestsByRoom(@PathVariable Long roomId) {
        List<CashPaymentRequest> requests = cashPaymentRequestService.getRequestsByRoom(roomId);
        List<CashPaymentRequestResponse> responses = requests.stream()
                .map(cashPaymentRequestMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get all pending requests (ADMIN)", description = "Retrieves all pending cash payment requests. This endpoint requires an authenticated user with ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/admin/pending")
    public ResponseEntity<List<CashPaymentRequestResponse>> getAllPendingRequests() {
        List<CashPaymentRequest> requests = cashPaymentRequestService.getAllPendingRequests();
        List<CashPaymentRequestResponse> responses = requests.stream()
                .map(cashPaymentRequestMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Verify cash request (ADMIN)", description = "Verifies a cash payment request. This endpoint requires an authenticated user with ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Verified successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/{id}/verify")
    public ResponseEntity<CashPaymentRequestResponse> verifyRequest(
            @PathVariable Long id,
            @Valid @RequestBody VerifyCashPaymentRequest request) {
        CashPaymentRequest cashPaymentRequest = cashPaymentRequestService.verifyRequest(id, request);
        CashPaymentRequestResponse response = cashPaymentRequestMapper.toResponse(cashPaymentRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reject cash request (ADMIN)", description = "Rejects a cash payment request with an optional admin note. This endpoint requires an authenticated user with ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Rejected successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/{id}/reject")
    public ResponseEntity<CashPaymentRequestResponse> rejectRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String adminNote) {
        CashPaymentRequest cashPaymentRequest = cashPaymentRequestService.rejectRequest(id, adminNote);
        CashPaymentRequestResponse response = cashPaymentRequestMapper.toResponse(cashPaymentRequest);
        return ResponseEntity.ok(response);
    }
}
