package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.ENUM.RoomStatus;
import com.pravin.maintenance_app.dto.CreateRoomRequest;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.repository.RoomRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private RoomService roomService;

    @Test
    void testGetAllRooms_ReturnsSortedList() {
        Room r1 = Room.builder().id(1L).roomNumber("101").status(RoomStatus.ACTIVE).build();
        Room r2 = Room.builder().id(2L).roomNumber("102").status(RoomStatus.ACTIVE).build();

        when(roomRepository.findAll(any(Sort.class))).thenReturn(List.of(r1, r2));

        List<Room> result = roomService.getAllRooms();

        assertEquals(2, result.size());
        assertEquals("101", result.get(0).getRoomNumber());
        assertEquals("102", result.get(1).getRoomNumber());
        verify(roomRepository).findAll(any(Sort.class));
    }

    @Test
    void testGetRoomById_Found() {
        Room r = Room.builder().id(5L).roomNumber("201").status(RoomStatus.ACTIVE).build();
        when(roomRepository.findById(5L)).thenReturn(Optional.of(r));

        Room result = roomService.getRoomById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("201", result.getRoomNumber());
    }

    @Test
    void testGetRoomById_NotFound_ThrowsException() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomService.getRoomById(999L));
    }

    @Test
    void testGetRoomByRoomNumber_Found() {
        Room r = Room.builder().id(1L).roomNumber("104").status(RoomStatus.ACTIVE).build();
        when(roomRepository.findByRoomNumber("104")).thenReturn(Optional.of(r));

        Room result = roomService.getRoomByRoomNumber("104");

        assertNotNull(result);
        assertEquals("104", result.getRoomNumber());
    }

    @Test
    void testAdminCanCreateRoom_Success() {
        User admin = User.builder().id(1L).role(Role.ADMIN).build();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(roomRepository.existsByRoomNumber("501")).thenReturn(false);

        Room savedRoom = Room.builder()
                .id(41L)
                .roomNumber("501")
                .status(RoomStatus.ACTIVE)
                .maintenanceExempt(false)
                .build();
        when(roomRepository.save(any(Room.class))).thenReturn(savedRoom);

        CreateRoomRequest request = CreateRoomRequest.builder()
                .roomNumber("501")
                .maintenanceExempt(false)
                .build();

        Room result = roomService.createRoom(request);

        assertNotNull(result);
        assertEquals(41L, result.getId());
        assertEquals("501", result.getRoomNumber());
        assertEquals(RoomStatus.ACTIVE, result.getStatus());
        assertFalse(result.isMaintenanceExempt());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void testResidentCannotCreateRoom_ThrowsAccessDeniedException() {
        User resident = User.builder().id(2L).role(Role.USER).build();
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        CreateRoomRequest request = CreateRoomRequest.builder()
                .roomNumber("501")
                .maintenanceExempt(false)
                .build();

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> roomService.createRoom(request)
        );

        assertEquals("Only admins can create rooms", ex.getMessage());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void testCreateRoom_DuplicateNumber_ThrowsBusinessValidationException() {
        User admin = User.builder().id(1L).role(Role.ADMIN).build();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(roomRepository.existsByRoomNumber("101")).thenReturn(true);

        CreateRoomRequest request = CreateRoomRequest.builder()
                .roomNumber("101")
                .maintenanceExempt(false)
                .build();

        BusinessValidationException ex = assertThrows(
                BusinessValidationException.class,
                () -> roomService.createRoom(request)
        );

        assertEquals("Room 101 already exists", ex.getMessage());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void testAdminCanUpdateRoomExemption_Success() {
        User admin = User.builder().id(1L).role(Role.ADMIN).build();
        Room existingRoom = Room.builder()
                .id(10L)
                .roomNumber("205")
                .maintenanceExempt(false)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(existingRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room updated = roomService.updateRoomExemption(10L, true);

        assertTrue(updated.isMaintenanceExempt());
        verify(roomRepository).save(existingRoom);
    }

    @Test
    void testResidentCannotUpdateRoomExemption_ThrowsAccessDeniedException() {
        User resident = User.builder().id(2L).role(Role.USER).build();
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> roomService.updateRoomExemption(10L, true)
        );

        assertEquals("Only admins can update room exemption", ex.getMessage());
        verify(roomRepository, never()).save(any());
    }
}
