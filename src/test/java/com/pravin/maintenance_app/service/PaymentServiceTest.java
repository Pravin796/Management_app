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
}
