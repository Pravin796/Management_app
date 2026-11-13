package com.pravin.maintenance_app.dto;

import com.pravin.maintenance_app.ENUM.NotificationCategory;
import com.pravin.maintenance_app.ENUM.NotificationTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    @NotBlank(message = "Notification title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @NotBlank(message = "Notification message is required")
    private String message;

    private NotificationCategory category;

    private NotificationTargetType targetType;

    private Long targetUserId;

    private Long targetRoomId;
}

