package com.pravin.maintenance_app.repository;

import com.pravin.maintenance_app.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

        Optional<Maintenance> findByRoomIdAndBillingMonth(
                        Long roomId,
                        YearMonth billingMonth);

        boolean existsByRoomIdAndBillingMonth(
                        Long roomId,
                        YearMonth billingMonth);

        List<Maintenance> findByRoomId(Long roomId);

        @Query("SELECT m FROM Maintenance m WHERE m.room.maintenanceExempt = false")
        List<Maintenance> findAllNonExemptMaintenance();
}