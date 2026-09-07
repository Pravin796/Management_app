package com.pravin.maintenance_app.repository;

import com.pravin.maintenance_app.ENUM.CashPaymentRequestStatus;
import com.pravin.maintenance_app.entity.CashPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashPaymentRequestRepository
        extends JpaRepository<CashPaymentRequest, Long> {

    List<CashPaymentRequest> findByRoomId(Long roomId);

    List<CashPaymentRequest> findByRoomIdAndStatus(
            Long roomId,
            CashPaymentRequestStatus status
    );

    List<CashPaymentRequest> findByStatus(
            CashPaymentRequestStatus status
    );
}