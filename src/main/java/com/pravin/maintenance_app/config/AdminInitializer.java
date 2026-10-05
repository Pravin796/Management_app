package com.pravin.maintenance_app.config;

import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.ENUM.UserStatus;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.repository.UserRepository;
import com.pravin.maintenance_app.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoomService roomService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.mobile-number}")
    private String adminMobileNumber;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.room-number}")
    private String adminRoomNumber;

    @Override
    public void run(String... args) {

        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        Room adminRoom = roomService.getRoomByRoomNumber(adminRoomNumber);

        User admin = new User();

        admin.setName(adminName);
        admin.setRoom(adminRoom);
        admin.setMobileNumber(adminMobileNumber);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        userRepository.save(admin);
    }
}