package com.pravin.maintenance_app.dto;

import com.pravin.maintenance_app.ENUM.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private RoomStatus status;
    private boolean maintenanceExempt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
