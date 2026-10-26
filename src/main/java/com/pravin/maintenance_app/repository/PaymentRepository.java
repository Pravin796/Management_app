package com.pravin.maintenance_app.repository;

import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.ENUM.PaymentMethod;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    long countByMethodAndStatus(PaymentMethod method, PaymentStatus status);

    List<Payment> findByRoomId(Long roomId);

    List<Payment> findByRoomIdAndStatus(
            Long roomId,
            PaymentStatus status
    );

    boolean existsByRoomIdAndStatus(Long roomId, PaymentStatus status);

    Optional<Payment> findByTransactionReference(
            String transactionReference
    );

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findFirstByRoomIdOrderByCreatedAtDescIdDesc(Long roomId);
}