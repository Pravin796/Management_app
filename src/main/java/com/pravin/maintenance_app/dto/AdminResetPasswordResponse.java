package com.pravin.maintenance_app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResetPasswordResponse {
    private Long userId;
    private String roomNumber;
    private String temporaryPassword;
    private String message;
}
