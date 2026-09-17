package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.CreateMaintenanceRequest;
import com.pravin.maintenance_app.dto.MaintenanceResponse;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.mapper.MaintenanceMapper;
import com.pravin.maintenance_app.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final MaintenanceMapper maintenanceMapper;

    @PostMapping
    public ResponseEntity<MaintenanceResponse> createMaintenance(
            @Valid @RequestBody CreateMaintenanceRequest request) {
        Maintenance maintenance = maintenanceService.createMaintenance(request);
        MaintenanceResponse response = maintenanceMapper.toResponse(maintenance);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> getMaintenanceById(@PathVariable Long id) {
        Maintenance maintenance = maintenanceService.getMaintenanceById(id);
        MaintenanceResponse response = maintenanceMapper.toResponse(maintenance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}/month/{billingMonth}")
    public ResponseEntity<MaintenanceResponse> getMaintenanceByRoomAndMonth(
            @PathVariable Long roomId,
            @PathVariable YearMonth billingMonth) {
        Maintenance maintenance = maintenanceService.getMaintenanceByRoomAndMonth(roomId, billingMonth);
        MaintenanceResponse response = maintenanceMapper.toResponse(maintenance);
        return ResponseEntity.ok(response);
    }
}
