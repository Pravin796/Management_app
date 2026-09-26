package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.ENUM.UserStatus;
import com.pravin.maintenance_app.dto.UserRegistrationRequest;
import com.pravin.maintenance_app.dto.UserResponse;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.mapper.UserMapper;
import com.pravin.maintenance_app.repository.UserRepository;
import jakarta.validation.Valid;
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

    public UserResponse registerUser(@Valid UserRegistrationRequest request) {

        // 1. Check duplicate mobile number
        if (userRepository.existsByMobileNumber(
                request.getMobileNumber())) {

            throw new com.pravin.maintenance_app.exception.BusinessValidationException(
                    "Mobile number is already registered"
            );
        }

        // 2. Find room
        Room room = roomService.getRoomByRoomNumber(
                request.getRoomNumber()
        );

        // 3. Check whether room already has an account
        if (userRepository.existsByRoomId(room.getId())) {

            throw new com.pravin.maintenance_app.exception.BusinessValidationException(
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
        user.setStatus(UserStatus.ACTIVE);

        // 6. Save user
        User savedUser = userRepository.save(user);

        // 7. Entity → Response DTO
        return userMapper.toResponse(savedUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.pravin.maintenance_app.exception.ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }
}