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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.pravin.maintenance_app.dto.ApiErrorResponse;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "Maintenance", description = "Endpoints for maintenance records")
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final MaintenanceMapper maintenanceMapper;

    @Operation(summary = "Create maintenance", description = "Creates a new maintenance record")
    @ApiResponse(responseCode = "201", description = "Created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping
    public ResponseEntity<MaintenanceResponse> createMaintenance(
            @Valid @RequestBody CreateMaintenanceRequest request) {
        Maintenance maintenance = maintenanceService.createMaintenance(request);
        MaintenanceResponse response = maintenanceMapper.toResponse(maintenance);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get maintenance by ID", description = "Retrieves a maintenance record by its ID")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> getMaintenanceById(@PathVariable Long id) {
        Maintenance maintenance = maintenanceService.getMaintenanceById(id);
        MaintenanceResponse response = maintenanceMapper.toResponse(maintenance);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get maintenance by room and month", description = "Retrieves a maintenance record for a specific room and billing month")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/room/{roomId}/month/{billingMonth}")
    public ResponseEntity<MaintenanceResponse> getMaintenanceByRoomAndMonth(
            @PathVariable Long roomId,
            @PathVariable YearMonth billingMonth) {
        Maintenance maintenance = maintenanceService.getMaintenanceByRoomAndMonth(roomId, billingMonth);
        MaintenanceResponse response = maintenanceMapper.toResponse(maintenance);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get my maintenance records", description = "Returns maintenance records belonging to the authenticated user's room. The room is determined from the authenticated JWT and should not be supplied by the client.")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/my")
    public ResponseEntity<List<MaintenanceResponse>> getMyMaintenance() {

        List<MaintenanceResponse> response = maintenanceService.getMyMaintenance()
                .stream()
                .map(maintenanceMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}
