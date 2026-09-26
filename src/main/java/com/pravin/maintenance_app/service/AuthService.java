package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.dto.LoginRequest;
import com.pravin.maintenance_app.dto.LoginResponse;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.repository.UserRepository;
import com.pravin.maintenance_app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {

        // 1. Authenticate mobile number + password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getMobileNumber(),
                        request.getPassword()));

        // 2. Get authenticated user
        User user = userRepository
                .findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // 3. Generate JWT
        String token = jwtService.generateToken(
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal());

        // 4. Create response
        LoginResponse response = new LoginResponse();

        response.setUserId(user.getId());
        response.setRoomNumber(user.getRoom().getRoomNumber());
        response.setMobileNumber(user.getMobileNumber());
        response.setRole(user.getRole().name());
        response.setToken(token);

        return response;
    }
}