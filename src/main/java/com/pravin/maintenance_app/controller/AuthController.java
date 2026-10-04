package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.UserRegistrationRequest;
import com.pravin.maintenance_app.dto.LoginRequest;
import com.pravin.maintenance_app.dto.LoginResponse;
import com.pravin.maintenance_app.dto.UserResponse;
import com.pravin.maintenance_app.service.AuthService;
import com.pravin.maintenance_app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.pravin.maintenance_app.dto.ApiErrorResponse;

@Tag(name = "Authentication", description = "Endpoints for user authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "Register user", description = "Register a new user")
    @SecurityRequirements()
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRegistrationRequest request) {

        UserResponse response = userService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Login user", description = "Login and receive a JWT token")
    @SecurityRequirements()
    @ApiResponse(responseCode = "200", description = "Successfully logged in")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}