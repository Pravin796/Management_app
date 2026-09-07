package com.pravin.maintenance_app.repository;

import com.pravin.maintenance_app.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.Optional;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    Optional<Maintenance> findByRoomIdAndBillingMonth(
            Long roomId,
            YearMonth billingMonth
    );

    boolean existsByRoomIdAndBillingMonth(
            Long roomId,
            YearMonth billingMonth
    );
}