package com.pravin.maintenance_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    @NotBlank(message = "Room number is required")
    @Size(max = 10, message = "Room number cannot exceed 10 characters")
    private String roomNumber;

    private boolean maintenanceExempt;
}
