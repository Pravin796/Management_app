package com.pravin.maintenance_app.entity;

import com.pravin.maintenance_app.ENUM.RoomStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

    @Entity
    @Table(
            name = "rooms",
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_room_room_number",
                            columnNames = "room_number"
                    )
            }
    )
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class Room {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "room_number", nullable = false, unique = true)
        private String roomNumber;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private RoomStatus status;

        @Column(name = "maintenance_exempt", nullable = false)
        private boolean maintenanceExempt;

        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;
    }

