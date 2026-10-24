package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.config.MaintenanceProperties;
import com.pravin.maintenance_app.dto.UserResponse;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.mapper.UserMapper;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import com.pravin.maintenance_app.repository.PaymentAllocationRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.repository.UserRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceAuthorizationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private MaintenanceProperties maintenanceProperties;

    @InjectMocks
    private UserService userService;

    @Test
    void testAdminCanAccessAnyUser() {
        User targetUser = new User();
        targetUser.setId(10L);
        targetUser.setName("Resident Bob");

        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setRole(Role.ADMIN);

        UserResponse mockResponse = new UserResponse();
        mockResponse.setId(10L);
        mockResponse.setName("Resident Bob");

        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(userMapper.toResponse(targetUser)).thenReturn(mockResponse);

        UserResponse response = userService.getUserById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Resident Bob", response.getName());
    }

    @Test
    void testResidentCanAccessOwnUserRecord() {
        User resident = new User();
        resident.setId(5L);
        resident.setRole(Role.USER);
        resident.setName("Alice");

        UserResponse mockResponse = new UserResponse();
        mockResponse.setId(5L);
        mockResponse.setName("Alice");

        when(userRepository.findById(5L)).thenReturn(Optional.of(resident));
        when(currentUserService.getCurrentUser()).thenReturn(resident);
        when(userMapper.toResponse(resident)).thenReturn(mockResponse);

        UserResponse response = userService.getUserById(5L);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Alice", response.getName());
    }

    @Test
    void testResidentCannotAccessOtherUserRecord_ThrowsAccessDeniedException() {
        User targetUser = new User();
        targetUser.setId(6L);
        targetUser.setName("Bob");

        User loggedInResident = new User();
        loggedInResident.setId(5L);
        loggedInResident.setRole(Role.USER);

        when(userRepository.findById(6L)).thenReturn(Optional.of(targetUser));
        when(currentUserService.getCurrentUser()).thenReturn(loggedInResident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> userService.getUserById(6L)
        );

        assertEquals("You are not allowed to access this user", ex.getMessage());
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void testUserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(999L)
        );
    }
}
