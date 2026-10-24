package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.dto.CreatePaymentAllocationRequest;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.PaymentAllocation;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.mapper.PaymentAllocationMapper;
import com.pravin.maintenance_app.repository.PaymentAllocationRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentAllocationServiceTest {

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MaintenanceService maintenanceService;

    @Mock
    private PaymentAllocationMapper paymentAllocationMapper;

    @Mock
    private PaymentService paymentService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PaymentAllocationService paymentAllocationService;

    @Test
    void testGetAllocationsByPayment_CallsPaymentServiceToEnforceOwnership() {
        Payment mockPayment = new Payment();
        mockPayment.setId(10L);

        when(paymentService.getPaymentById(10L)).thenReturn(mockPayment);
        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setId(1L);
        allocation.setAmount(new BigDecimal("500.00"));
        when(paymentAllocationRepository.findByPaymentId(10L)).thenReturn(List.of(allocation));

        List<PaymentAllocation> results = paymentAllocationService.getAllocationsByPayment(10L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(paymentService).getPaymentById(10L);
    }

    @Test
    void testGetAllocationsByPayment_WhenPaymentServiceDeniesAccess_ThrowsAccessDeniedException() {
        when(paymentService.getPaymentById(10L))
                .thenThrow(new AccessDeniedException("You are not allowed to access this payment"));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> paymentAllocationService.getAllocationsByPayment(10L)
        );

        assertEquals("You are not allowed to access this payment", ex.getMessage());
        verify(paymentAllocationRepository, never()).findByPaymentId(any());
    }

    @Test
    void testCreateAllocation_ResidentForbidden_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setRole(Role.USER);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        CreatePaymentAllocationRequest request = new CreatePaymentAllocationRequest();
        request.setPaymentId(10L);
        request.setMaintenanceId(20L);
        request.setAmount(new BigDecimal("500.00"));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> paymentAllocationService.createAllocation(request)
        );

        assertEquals("Only ADMIN can create payment allocations", ex.getMessage());
        verify(paymentRepository, never()).findById(any());
        verify(paymentAllocationRepository, never()).save(any());
    }

    @Test
    void testCreateAllocation_AdminAllowed_SuccessfullyAllocates() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Room room = new Room();
        room.setId(100L);

        Payment payment = new Payment();
        payment.setId(10L);
        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setRoom(room);

        Maintenance maintenance = new Maintenance();
        maintenance.setId(20L);
        maintenance.setStatus(MaintenanceStatus.PENDING);
        maintenance.setAmount(new BigDecimal("500.00"));
        maintenance.setRoom(room);

        CreatePaymentAllocationRequest request = new CreatePaymentAllocationRequest();
        request.setPaymentId(10L);
        request.setMaintenanceId(20L);
        request.setAmount(new BigDecimal("500.00"));

        PaymentAllocation allocationEntity = new PaymentAllocation();
        allocationEntity.setId(1L);
        allocationEntity.setPayment(payment);
        allocationEntity.setMaintenance(maintenance);
        allocationEntity.setAmount(new BigDecimal("500.00"));

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(maintenanceService.getMaintenanceById(20L)).thenReturn(maintenance);
        when(paymentAllocationRepository.getVerifiedAmountByPaymentId(10L)).thenReturn(BigDecimal.ZERO);
        when(paymentAllocationRepository.getVerifiedAmountByMaintenanceId(20L)).thenReturn(BigDecimal.ZERO);
        when(paymentAllocationMapper.toEntity(request)).thenReturn(allocationEntity);
        when(paymentAllocationRepository.save(any(PaymentAllocation.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentAllocation saved = paymentAllocationService.createAllocation(request);

        assertNotNull(saved);
        assertEquals(new BigDecimal("500.00"), saved.getAmount());
        verify(paymentAllocationRepository).save(any(PaymentAllocation.class));
    }
}
