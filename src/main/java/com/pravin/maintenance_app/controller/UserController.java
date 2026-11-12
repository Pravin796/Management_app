package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.UserDashboardResponse;
import com.pravin.maintenance_app.dto.UserResponse;
import com.pravin.maintenance_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.pravin.maintenance_app.dto.ApiErrorResponse;

@Tag(name = "Users", description = "Endpoints for user management")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get user by ID", description = "Retrieves user details by ID")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get resident dashboard",
            description = "Retrieves the authenticated resident's dashboard details including room, maintenance rate, outstanding dues, current month maintenance, and most recent payment. The resident is derived from the authenticated JWT principal.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/me/dashboard")
    public ResponseEntity<UserDashboardResponse> getUserDashboard() {
        UserDashboardResponse response = userService.getUserDashboard();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get current user profile",
            description = "Retrieves the authenticated resident's profile information.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = userService.getCurrentUserProfile();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Upload profile picture",
            description = "Uploads and updates the profile picture for the authenticated user via multipart/form-data.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Profile photo updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid image file", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        UserResponse response = userService.uploadProfilePicture(file);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove profile picture",
            description = "Removes the authenticated user's profile picture.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Profile photo removed successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @DeleteMapping("/me/profile-picture")
    public ResponseEntity<UserResponse> removeProfilePicture() {
        UserResponse response = userService.removeProfilePicture();
        return ResponseEntity.ok(response);
    }
}
