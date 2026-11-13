package com.pravin.maintenance_app.dto;

import com.pravin.maintenance_app.ENUM.NotificationCategory;
import com.pravin.maintenance_app.ENUM.NotificationTargetType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private Long notificationId;
    private String title;
    private String message;
    private NotificationCategory category;
    private NotificationTargetType targetType;
    private Long targetUserId;
    private String targetUserName;
    private Long targetRoomId;
    private String targetRoomNumber;
    private String createdByName;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}

