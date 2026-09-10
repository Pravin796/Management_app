package com.pravin.maintenance_app.repository;

import com.pravin.maintenance_app.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentAllocationRepository
        extends JpaRepository<PaymentAllocation, Long> {

    List<PaymentAllocation> findByMaintenanceId(Long maintenanceId);

    List<PaymentAllocation> findByPaymentId(Long paymentId);

    @Query("""
            SELECT COALESCE(SUM(pa.amount), 0)
            FROM PaymentAllocation pa
            WHERE pa.maintenance.id = :maintenanceId
            AND pa.payment.status = com.pravin.maintenance_app.ENUM.PaymentStatus.VERIFIED
            """)
    BigDecimal getVerifiedAmountByMaintenanceId(
            @Param("maintenanceId") Long maintenanceId
    );

    @Query("""
        SELECT COALESCE(SUM(pa.amount), 0)
        FROM PaymentAllocation pa
        WHERE pa.payment.id = :paymentId
        AND pa.payment.status =
            com.pravin.maintenance_app.ENUM.PaymentStatus.VERIFIED
        """)
    BigDecimal getVerifiedAmountByPaymentId(
            @Param("paymentId") Long paymentId
    );
}