package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.AdminDashboardResponse;
import com.pravin.maintenance_app.dto.ApiErrorResponse;
import com.pravin.maintenance_app.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Dashboard", description = "Admin dashboard overview endpoints")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(
            summary = "Get admin dashboard overview",
            description = "Retrieves aggregated metrics for admin overview including total rooms, collected maintenance, outstanding balance, pending online payments, pending cash requests, and verified cash payments.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        AdminDashboardResponse response = adminDashboardService.getAdminDashboard();
        return ResponseEntity.ok(response);
    }
}
