package com.pravin.maintenance_app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterPushTokenRequest {

    @NotBlank(message = "Push token is required")
    private String pushToken;

    private String deviceType;
}

