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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRegistrationRequest request) {

        UserResponse response = userService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}