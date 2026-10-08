package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.AdminResetPasswordResponse;
import com.pravin.maintenance_app.dto.ApiErrorResponse;
import com.pravin.maintenance_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Users", description = "Admin endpoints for user management")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "Admin Reset Password", description = "Admin resets a user's password")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<AdminResetPasswordResponse> resetPassword(@PathVariable Long userId) {
        AdminResetPasswordResponse response = userService.adminResetPassword(userId);
        return ResponseEntity.ok(response);
    }
}
