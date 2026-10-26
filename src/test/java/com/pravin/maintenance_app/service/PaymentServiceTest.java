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
import com.pravin.maintenance_app.exception.BusinessValidationException;

import java.util.List;
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

    @Test
    void testGetPaymentsByRoom_AdminAllowed() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Payment payment = new Payment();
        payment.setId(10L);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(paymentRepository.findByRoomId(20L)).thenReturn(List.of(payment));

        List<Payment> results = paymentService.getPaymentsByRoom(20L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(roomService).getRoomById(20L);
        verify(paymentRepository).findByRoomId(20L);
    }

    @Test
    void testGetPaymentsByRoom_ResidentOwnRoomAllowed() {
        Room myRoom = new Room();
        myRoom.setId(20L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(myRoom);

        Payment payment = new Payment();
        payment.setId(10L);

        when(currentUserService.getCurrentUser()).thenReturn(resident);
        when(paymentRepository.findByRoomId(20L)).thenReturn(List.of(payment));

        List<Payment> results = paymentService.getPaymentsByRoom(20L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(roomService).getRoomById(20L);
        verify(paymentRepository).findByRoomId(20L);
    }

    @Test
    void testGetPaymentsByRoom_ResidentOtherRoomForbidden_ThrowsAccessDeniedException() {
        Room myRoom = new Room();
        myRoom.setId(20L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(myRoom);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getPaymentsByRoom(99L)
        );

        assertEquals("You are not allowed to access payments for this room", ex.getMessage());
        verify(roomService).getRoomById(99L);
        verify(paymentRepository, never()).findByRoomId(any());
    }

    @Test
    void testGetAllPendingPayments_AdminAllowed() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Payment p = new Payment();
        p.setId(10L);
        p.setStatus(PaymentStatus.PENDING);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(paymentRepository.findByStatus(PaymentStatus.PENDING)).thenReturn(List.of(p));

        List<Payment> results = paymentService.getAllPendingPayments();

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(paymentRepository).findByStatus(PaymentStatus.PENDING);
    }

    @Test
    void testGetAllPendingPayments_ResidentForbidden_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setRole(Role.USER);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getAllPendingPayments()
        );

        assertEquals("Only ADMIN can access all pending payments", ex.getMessage());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    void testRejectPayment_AdminAllowed() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Payment payment = new Payment();
        payment.setId(10L);
        payment.setStatus(PaymentStatus.PENDING);
        Room room = new Room();
        room.setId(20L);
        payment.setRoom(room);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.rejectPayment(10L);

        assertNotNull(result);
        assertEquals(PaymentStatus.REJECTED, result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testRejectPayment_ResidentForbidden_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setRole(Role.USER);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> paymentService.rejectPayment(10L)
        );

        assertEquals("Only ADMIN can reject payments", ex.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testRejectPayment_NonPending_ThrowsBusinessValidationException() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Payment payment = new Payment();
        payment.setId(10L);
        payment.setStatus(PaymentStatus.VERIFIED);
        Room room = new Room();
        room.setId(20L);
        payment.setRoom(room);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        BusinessValidationException ex = assertThrows(
                BusinessValidationException.class,
                () -> paymentService.rejectPayment(10L)
        );

        assertEquals("Only pending payments can be rejected", ex.getMessage());
        verify(paymentRepository, never()).save(any());
    }
}
