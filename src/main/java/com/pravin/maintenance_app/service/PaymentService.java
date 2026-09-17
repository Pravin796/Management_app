package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.dto.CreatePaymentRequest;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.PaymentMapper;
import com.pravin.maintenance_app.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
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

    public Payment createPayment(CreatePaymentRequest request) {

        Room room = roomService.getRoomById(request.getRoomId());

        Payment payment = paymentMapper.toEntity(request);

        payment.setRoom(room);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment getPaymentById(Long paymentId) {

        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found with id: " + paymentId
                        )
                );
    }

    public List<Payment> getPaymentsByRoom(Long roomId) {

        roomService.getRoomById(roomId);

        return paymentRepository.findByRoomId(roomId);
    }

    public List<Payment> getPendingPaymentsByRoom(Long roomId) {

        roomService.getRoomById(roomId);

        return paymentRepository.findByRoomIdAndStatus(
                roomId,
                PaymentStatus.PENDING
        );
    }

    @Transactional
    public Payment verifyPayment(Long paymentId) {

        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException(
                    "Only pending payments can be verified"
            );
        }

        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }
}