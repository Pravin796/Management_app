package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.dto.CreatePaymentRequest;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.PaymentMapper;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import com.pravin.maintenance_app.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RoomService roomService;
    private final PaymentMapper paymentMapper;
    private final CurrentUserService currentUserService;

    public Payment createPayment(CreatePaymentRequest request) {

    User currentUser = currentUserService.getCurrentUser();

    Room room = currentUser.getRoom();

    if (paymentRepository.existsByRoomIdAndStatus(
            room.getId(),
            PaymentStatus.PENDING)) {

        throw new BusinessValidationException(
                "Room already has a pending payment");
    }

    Payment payment = paymentMapper.toEntity(request);

    payment.setRoom(room);
    payment.setStatus(PaymentStatus.PENDING);
    payment.setCreatedAt(LocalDateTime.now());
    payment.setUpdatedAt(LocalDateTime.now());

    return paymentRepository.save(payment);
}

    public Payment getPaymentById(Long paymentId) {

    Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Payment not found with id: " + paymentId
                    )
            );

    User currentUser = currentUserService.getCurrentUser();

    if (!currentUser.getRoom().getId()
            .equals(payment.getRoom().getId())) {

        throw new AccessDeniedException(
                "You are not allowed to access this payment"
        );
    }

    return payment;
}

    public List<Payment> getPaymentsByRoom(Long roomId) {

        roomService.getRoomById(roomId);

        return paymentRepository.findByRoomId(roomId);
    }

    public List<Payment> getPendingPaymentsByRoom(Long roomId) {

        roomService.getRoomById(roomId);

        return paymentRepository.findByRoomIdAndStatus(
                roomId,
                PaymentStatus.PENDING);
    }

    @Transactional
    public Payment updatePaymentProof(Long paymentId,
            com.pravin.maintenance_app.dto.UpdatePaymentProofRequest request) {

        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessValidationException("Only pending payments can have payment proof updated.");
        }

        if ((request.getTransactionReference() == null || request.getTransactionReference().isBlank()) &&
                (request.getScreenshotUrl() == null || request.getScreenshotUrl().isBlank())) {
            throw new BusinessValidationException(
                    "At least one proof field (transactionReference or screenshotUrl) must be provided");
        }

        if (request.getTransactionReference() != null && !request.getTransactionReference().isBlank()) {
            payment.setTransactionReference(request.getTransactionReference());
        }

        if (request.getScreenshotUrl() != null && !request.getScreenshotUrl().isBlank()) {
            payment.setScreenshotUrl(request.getScreenshotUrl());
        }

        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment verifyPayment(Long paymentId) {

        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessValidationException(
                    "Only pending payments can be verified");
        }

        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public List<Payment> getMyPayments() {

        User user = currentUserService.getCurrentUser();
        Long roomId = user.getRoom().getId();
        return paymentRepository.findByRoomId(roomId);
    }
}
