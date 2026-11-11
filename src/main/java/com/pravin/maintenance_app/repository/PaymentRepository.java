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

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.room.id = :roomId AND p.status = com.pravin.maintenance_app.ENUM.PaymentStatus.PENDING AND (p.transactionReference IS NOT NULL OR p.screenshotUrl IS NOT NULL)")
    boolean existsSubmittedPaymentByRoomId(@org.springframework.data.repository.query.Param("roomId") Long roomId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p WHERE p.room.id = :roomId AND p.status = com.pravin.maintenance_app.ENUM.PaymentStatus.PENDING AND (p.transactionReference IS NOT NULL OR p.screenshotUrl IS NOT NULL) ORDER BY p.createdAt DESC")
    List<Payment> findSubmittedPaymentsByRoomId(@org.springframework.data.repository.query.Param("roomId") Long roomId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p WHERE p.room.id = :roomId AND p.status = com.pravin.maintenance_app.ENUM.PaymentStatus.PENDING AND p.transactionReference IS NULL AND p.screenshotUrl IS NULL ORDER BY p.createdAt DESC")
    List<Payment> findIncompletePaymentsByRoomId(@org.springframework.data.repository.query.Param("roomId") Long roomId);
}