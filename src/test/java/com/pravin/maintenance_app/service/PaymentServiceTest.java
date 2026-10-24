package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.dto.PaymentResponse;
import com.pravin.maintenance_app.dto.UpdatePaymentProofRequest;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.mapper.PaymentMapper;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;
    
    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private PaymentService paymentService;

    private User mockUser() {
        User user = new User();
        user.setRole(Role.ADMIN);
        Room room = new Room();
        room.setId(10L);
        user.setRoom(room);
        return user;
    }

    @Test
    void testExistingPaymentWithoutScreenshotPublicIdStillWorks() {
        // existing payment proof behavior is not broken
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(PaymentStatus.PENDING);
        Room room = new Room();
        room.setId(10L);
        payment.setRoom(room);
        
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(currentUserService.getCurrentUser()).thenReturn(mockUser());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdatePaymentProofRequest request = new UpdatePaymentProofRequest();
        request.setTransactionReference("TXN123");
        
        Payment updatedPayment = paymentService.updatePaymentProof(1L, request);
        
        assertEquals("TXN123", updatedPayment.getTransactionReference());
        assertNull(updatedPayment.getScreenshotPublicId());
    }

    @Test
    void testPaymentCanContainScreenshotPublicId() {
        Payment payment = new Payment();
        payment.setScreenshotPublicId("maintenance-app/payment-screenshots/abc");
        assertEquals("maintenance-app/payment-screenshots/abc", payment.getScreenshotPublicId());
    }

    @Test
    void testPaymentResponseReturnsScreenshotPublicId() {
        PaymentResponse response = new PaymentResponse();
        response.setScreenshotPublicId("maintenance-app/payment-screenshots/abc");
        assertEquals("maintenance-app/payment-screenshots/abc", response.getScreenshotPublicId());
    }

    @Test
    void testPaymentProofUpdateCanPersistScreenshotPublicId() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(PaymentStatus.PENDING);
        Room room = new Room();
        room.setId(10L);
        payment.setRoom(room);
        
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(currentUserService.getCurrentUser()).thenReturn(mockUser());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdatePaymentProofRequest request = new UpdatePaymentProofRequest();
        request.setScreenshotPublicId("maintenance-app/payment-screenshots/abc");
        
        Payment updatedPayment = paymentService.updatePaymentProof(1L, request);
        
        assertEquals("maintenance-app/payment-screenshots/abc", updatedPayment.getScreenshotPublicId());
    }

    @Test
    void testAdminCanAccessPaymentForAnyRoom() {
        Payment payment = new Payment();
        payment.setId(10L);
        Room room = new Room();
        room.setId(20L);
        payment.setRoom(room);

        User admin = new User();
        admin.setRole(Role.ADMIN);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        Payment result = paymentService.getPaymentById(10L);
        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void testResidentCanAccessPaymentForOwnRoom() {
        Payment payment = new Payment();
        payment.setId(10L);
        Room room = new Room();
        room.setId(20L);
        payment.setRoom(room);

        User resident = new User();
        resident.setRole(Role.USER);
        Room residentRoom = new Room();
        residentRoom.setId(20L);
        resident.setRoom(residentRoom);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        Payment result = paymentService.getPaymentById(10L);
        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void testResidentCannotAccessPaymentForDifferentRoom_ThrowsAccessDeniedException() {
        Payment payment = new Payment();
        payment.setId(10L);
        Room room = new Room();
        room.setId(20L);
        payment.setRoom(room);

        User resident = new User();
        resident.setRole(Role.USER);
        Room otherRoom = new Room();
        otherRoom.setId(99L);
        resident.setRoom(otherRoom);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getPaymentById(10L)
        );
        assertEquals("You are not allowed to access this payment", ex.getMessage());
    }

    @Test
    void testAdminCanVerifyPendingPayment() {
        Payment payment = new Payment();
        payment.setId(10L);
        payment.setStatus(PaymentStatus.PENDING);
        Room room = new Room();
        room.setId(20L);
        payment.setRoom(room);

        User admin = new User();
        admin.setRole(Role.ADMIN);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        Payment verified = paymentService.verifyPayment(10L);
        assertEquals(PaymentStatus.VERIFIED, verified.getStatus());
        assertNotNull(verified.getVerifiedAt());
    }

    @Test
    void testResidentCannotVerifyPayment_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setRole(Role.USER);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> paymentService.verifyPayment(10L)
        );
        assertEquals("Only ADMIN can verify payments", ex.getMessage());
        verify(paymentRepository, never()).save(any());
    }
}
