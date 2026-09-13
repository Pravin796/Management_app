package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.dto.LoginRequest;
import com.pravin.maintenance_app.dto.LoginResponse;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {

        // 1. Find user by mobile number
        User user = userRepository
                .findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() ->
                        new RuntimeException("Invalid mobile number or password")
                );

        // 2. Check password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid mobile number or password"
            );
        }

        // 3. Check whether user is active
        if (user.getStatus() != com.pravin.maintenance_app.ENUM.UserStatus.ACTIVE) {
            throw new RuntimeException("User account is inactive");
        }

        // 4. Create response
        LoginResponse response = new LoginResponse();

        response.setUserId(user.getId());
        response.setRoomNumber(user.getRoom().getRoomNumber());
        response.setMobileNumber(user.getMobileNumber());
        response.setRole(user.getRole().name());

        return response;
    }
}