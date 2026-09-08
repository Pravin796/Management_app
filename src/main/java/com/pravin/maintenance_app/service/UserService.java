package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.dto.RegisterUserRequest;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.mapper.UserMapper;
import com.pravin.maintenance_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoomService roomService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(RegisterUserRequest request) {

        // 1. Check duplicate mobile number
        if (userRepository.existsByMobileNumber(
                request.getMobileNumber())) {

            throw new RuntimeException(
                    "Mobile number is already registered"
            );
        }

        // 2. Find room
        Room room = roomService.getRoomByRoomNumber(
                request.getRoomNumber()
        );

        // 3. Check whether room already has an account
        if (userRepository.existsByRoomId(room.getId())) {

            throw new RuntimeException(
                    "An account already exists for this room"
            );
        }

        // 4. Map DTO → Entity
        User user = userMapper.toEntity(request);

        // 5. Set business/security-sensitive fields
        user.setRoom(room);
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(Role.USER);

        // 6. Save user
        return userRepository.save(user);
    }
}