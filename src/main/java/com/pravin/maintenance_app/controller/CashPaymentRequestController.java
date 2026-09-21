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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cash-payments")
@RequiredArgsConstructor
public class CashPaymentRequestController {

    private final CashPaymentRequestService cashPaymentRequestService;
    private final CashPaymentRequestMapper cashPaymentRequestMapper;

    @PostMapping
    public ResponseEntity<CashPaymentRequestResponse> createRequest(
            @Valid @RequestBody CreateCashPaymentRequest request) {
        CashPaymentRequest cashPaymentRequest = cashPaymentRequestService.createRequest(request);
        CashPaymentRequestResponse response = cashPaymentRequestMapper.toResponse(cashPaymentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CashPaymentRequestResponse> getRequestById(@PathVariable Long id) {
        CashPaymentRequest cashPaymentRequest = cashPaymentRequestService.getRequestById(id);
        CashPaymentRequestResponse response = cashPaymentRequestMapper.toResponse(cashPaymentRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<CashPaymentRequestResponse>> getRequestsByRoom(@PathVariable Long roomId) {
        List<CashPaymentRequest> requests = cashPaymentRequestService.getRequestsByRoom(roomId);
        List<CashPaymentRequestResponse> responses = requests.stream()
                .map(cashPaymentRequestMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<List<CashPaymentRequestResponse>> getAllPendingRequests() {
        List<CashPaymentRequest> requests = cashPaymentRequestService.getAllPendingRequests();
        List<CashPaymentRequestResponse> responses = requests.stream()
                .map(cashPaymentRequestMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<CashPaymentRequestResponse> verifyRequest(
            @PathVariable Long id,
            @Valid @RequestBody VerifyCashPaymentRequest request) {
        CashPaymentRequest cashPaymentRequest = cashPaymentRequestService.verifyRequest(id, request);
        CashPaymentRequestResponse response = cashPaymentRequestMapper.toResponse(cashPaymentRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<CashPaymentRequestResponse> rejectRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String adminNote) {
        CashPaymentRequest cashPaymentRequest = cashPaymentRequestService.rejectRequest(id, adminNote);
        CashPaymentRequestResponse response = cashPaymentRequestMapper.toResponse(cashPaymentRequest);
        return ResponseEntity.ok(response);
    }
}
