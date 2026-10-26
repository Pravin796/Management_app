package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.config.MaintenanceProperties;
import com.pravin.maintenance_app.dto.CreateMaintenanceRequest;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.mapper.MaintenanceMapper;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MaintenanceServiceTest {

    @Mock
    private MaintenanceProperties maintenanceProperties;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private MaintenanceMapper maintenanceMapper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MaintenanceService maintenanceService;

    @Test
    void testCreateMaintenance_ResidentForbidden_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setRole(Role.USER);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        CreateMaintenanceRequest request = new CreateMaintenanceRequest();
        request.setRoomId(1L);
        request.setBillingMonth(YearMonth.of(2026, 11));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> maintenanceService.createMaintenance(request)
        );

        assertEquals("Only ADMIN can create maintenance records", ex.getMessage());
        verify(roomService, never()).getRoomById(any());
        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    void testCreateMaintenance_AdminAllowed_SuccessfullyCreates() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Room room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setMaintenanceExempt(false);

        YearMonth billingMonth = YearMonth.of(2026, 11);
        CreateMaintenanceRequest request = new CreateMaintenanceRequest();
        request.setRoomId(1L);
        request.setBillingMonth(billingMonth);

        Maintenance mappedMaintenance = new Maintenance();
        mappedMaintenance.setBillingMonth(billingMonth);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(roomService.getRoomById(1L)).thenReturn(room);
        when(maintenanceRepository.existsByRoomIdAndBillingMonth(1L, billingMonth)).thenReturn(false);
        when(maintenanceProperties.getMonthlyAmount()).thenReturn(new BigDecimal("500.00"));
        when(maintenanceMapper.toEntity(request)).thenReturn(mappedMaintenance);
        when(maintenanceRepository.save(any(Maintenance.class))).thenAnswer(i -> i.getArguments()[0]);

        Maintenance created = maintenanceService.createMaintenance(request);

        assertNotNull(created);
        assertEquals(room, created.getRoom());
        assertEquals(new BigDecimal("500.00"), created.getAmount());
        assertEquals(MaintenanceStatus.PENDING, created.getStatus());
        verify(maintenanceRepository).save(any(Maintenance.class));
    }

    @Test
    void testCreateMaintenance_ExemptRoom_ThrowsBusinessValidationException() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Room room = new Room();
        room.setId(2L);
        room.setRoomNumber("102");
        room.setMaintenanceExempt(true);

        CreateMaintenanceRequest request = new CreateMaintenanceRequest();
        request.setRoomId(2L);
        request.setBillingMonth(YearMonth.of(2026, 11));

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(roomService.getRoomById(2L)).thenReturn(room);

        BusinessValidationException ex = assertThrows(
                BusinessValidationException.class,
                () -> maintenanceService.createMaintenance(request)
        );

        assertTrue(ex.getMessage().contains("Maintenance is exempt for room: 102"));
        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    void testCreateMaintenance_DuplicateMonth_ThrowsBusinessValidationException() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Room room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setMaintenanceExempt(false);

        YearMonth billingMonth = YearMonth.of(2026, 11);
        CreateMaintenanceRequest request = new CreateMaintenanceRequest();
        request.setRoomId(1L);
        request.setBillingMonth(billingMonth);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(roomService.getRoomById(1L)).thenReturn(room);
        when(maintenanceRepository.existsByRoomIdAndBillingMonth(1L, billingMonth)).thenReturn(true);

        BusinessValidationException ex = assertThrows(
                BusinessValidationException.class,
                () -> maintenanceService.createMaintenance(request)
        );

        assertTrue(ex.getMessage().contains("Maintenance already exists for room: 101"));
        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    void testGetMaintenanceById_AdminAllowed() {
        Maintenance maintenance = new Maintenance();
        maintenance.setId(50L);
        Room room = new Room();
        room.setId(10L);
        maintenance.setRoom(room);

        User admin = new User();
        admin.setRole(Role.ADMIN);

        when(maintenanceRepository.findById(50L)).thenReturn(Optional.of(maintenance));
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        Maintenance result = maintenanceService.getMaintenanceById(50L);
        assertNotNull(result);
        assertEquals(50L, result.getId());
    }

    @Test
    void testGetMaintenanceById_ResidentOwnRoomAllowed() {
        Maintenance maintenance = new Maintenance();
        maintenance.setId(50L);
        Room room = new Room();
        room.setId(10L);
        maintenance.setRoom(room);

        User resident = new User();
        resident.setRole(Role.USER);
        Room residentRoom = new Room();
        residentRoom.setId(10L);
        resident.setRoom(residentRoom);

        when(maintenanceRepository.findById(50L)).thenReturn(Optional.of(maintenance));
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        Maintenance result = maintenanceService.getMaintenanceById(50L);
        assertNotNull(result);
        assertEquals(50L, result.getId());
    }

    @Test
    void testGetMaintenanceById_ResidentOtherRoomForbidden_ThrowsAccessDeniedException() {
        Maintenance maintenance = new Maintenance();
        maintenance.setId(50L);
        Room room = new Room();
        room.setId(10L);
        maintenance.setRoom(room);

        User resident = new User();
        resident.setRole(Role.USER);
        Room otherRoom = new Room();
        otherRoom.setId(99L);
        resident.setRoom(otherRoom);

        when(maintenanceRepository.findById(50L)).thenReturn(Optional.of(maintenance));
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> maintenanceService.getMaintenanceById(50L)
        );

        assertEquals("You are not allowed to access this maintenance record", ex.getMessage());
    }

    @Test
    void testGetMaintenanceByRoomAndMonth_ResidentOtherRoomForbidden_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setRole(Role.USER);
        Room residentRoom = new Room();
        residentRoom.setId(10L);
        resident.setRoom(residentRoom);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> maintenanceService.getMaintenanceByRoomAndMonth(20L, YearMonth.of(2026, 10))
        );

        assertEquals("You are not allowed to access maintenance for this room", ex.getMessage());
        verify(maintenanceRepository, never()).findByRoomIdAndBillingMonth(any(), any());
    }

    @Test
    void testGetMaintenanceByRoomAndMonth_AdminAllowed() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Maintenance maintenance = new Maintenance();
        maintenance.setId(50L);

        YearMonth month = YearMonth.of(2026, 10);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(maintenanceRepository.findByRoomIdAndBillingMonth(20L, month)).thenReturn(Optional.of(maintenance));

        Maintenance result = maintenanceService.getMaintenanceByRoomAndMonth(20L, month);
        assertNotNull(result);
        assertEquals(50L, result.getId());
    }

    @Test
    void testGetMaintenanceByRoomAndMonth_ResidentOwnRoomAllowed() {
        Room myRoom = new Room();
        myRoom.setId(20L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(myRoom);

        Maintenance maintenance = new Maintenance();
        maintenance.setId(50L);

        YearMonth month = YearMonth.of(2026, 10);
        when(currentUserService.getCurrentUser()).thenReturn(resident);
        when(maintenanceRepository.findByRoomIdAndBillingMonth(20L, month)).thenReturn(Optional.of(maintenance));

        Maintenance result = maintenanceService.getMaintenanceByRoomAndMonth(20L, month);
        assertNotNull(result);
        assertEquals(50L, result.getId());
    }

    @Test
    void testGetMaintenanceByRoom_AdminAllowed() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Maintenance m = new Maintenance();
        m.setId(10L);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(maintenanceRepository.findByRoomId(20L)).thenReturn(List.of(m));

        List<Maintenance> results = maintenanceService.getMaintenanceByRoom(20L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(maintenanceRepository).findByRoomId(20L);
    }

    @Test
    void testGetMaintenanceByRoom_ResidentOtherRoomForbidden() {
        Room myRoom = new Room();
        myRoom.setId(20L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(myRoom);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> maintenanceService.getMaintenanceByRoom(99L)
        );

        assertEquals("You are not allowed to access maintenance for this room", ex.getMessage());
        verify(maintenanceRepository, never()).findByRoomId(any());
    }
}
