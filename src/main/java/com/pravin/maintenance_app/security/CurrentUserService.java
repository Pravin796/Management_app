package com.pravin.maintenance_app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String mobileNumber = authentication.getName();

        return userRepository
                .findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Authenticated user not found"));
    }
}
