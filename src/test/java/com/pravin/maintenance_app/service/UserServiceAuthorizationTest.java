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
import com.pravin.maintenance_app.dto.AdminResetPasswordResponse;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.exception.BusinessValidationException;

import java.util.List;
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

    @Test
    void testAdminCanResetResidentPassword() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        Room room = new Room();
        room.setId(101L);
        room.setRoomNumber("101");

        User resident = new User();
        resident.setId(2L);
        resident.setRole(Role.USER);
        resident.setRoom(room);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(resident));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminResetPasswordResponse response = userService.adminResetPassword(2L);

        assertNotNull(response);
        assertEquals(2L, response.getUserId());
        assertEquals("101", response.getRoomNumber());
        assertNotNull(response.getTemporaryPassword());
        assertEquals(10, response.getTemporaryPassword().length());
        assertTrue(resident.isMustChangePassword());
        verify(userRepository).save(resident);
    }

    @Test
    void testResidentCannotResetPassword_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setId(2L);
        resident.setRole(Role.USER);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> userService.adminResetPassword(3L)
        );

        assertEquals("Only admins can reset user passwords", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testAdminCannotResetAdminPassword_ThrowsBusinessValidationException() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        User targetAdmin = new User();
        targetAdmin.setId(99L);
        targetAdmin.setRole(Role.ADMIN);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(99L)).thenReturn(Optional.of(targetAdmin));

        BusinessValidationException ex = assertThrows(
                BusinessValidationException.class,
                () -> userService.adminResetPassword(99L)
        );

        assertEquals("Cannot reset password for ADMIN accounts", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testAdminCanGetAllUsers() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        User u1 = new User();
        u1.setId(1L);
        User u2 = new User();
        u2.setId(2L);

        UserResponse r1 = new UserResponse();
        r1.setId(1L);
        UserResponse r2 = new UserResponse();
        r2.setId(2L);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        when(userMapper.toResponse(u1)).thenReturn(r1);
        when(userMapper.toResponse(u2)).thenReturn(r2);

        List<UserResponse> list = userService.getAllUsers();

        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getId());
        assertEquals(2L, list.get(1).getId());
    }

    @Test
    void testResidentCannotGetAllUsers_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setId(2L);
        resident.setRole(Role.USER);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        assertThrows(
                AccessDeniedException.class,
                () -> userService.getAllUsers()
        );
        verify(userRepository, never()).findAll();
    }

    @Test
    void testAdminCanGetUserByRoomId() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        User resident = new User();
        resident.setId(5L);

        UserResponse response = new UserResponse();
        response.setId(5L);
        response.setRoomId(101L);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findByRoomId(101L)).thenReturn(Optional.of(resident));
        when(userMapper.toResponse(resident)).thenReturn(response);

        UserResponse result = userService.getUserByRoomId(101L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
    }

    @Test
    void testResidentCannotGetUserByRoomId_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setId(2L);
        resident.setRole(Role.USER);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        assertThrows(
                AccessDeniedException.class,
                () -> userService.getUserByRoomId(101L)
        );
        verify(userRepository, never()).findByRoomId(any());
    }
}
