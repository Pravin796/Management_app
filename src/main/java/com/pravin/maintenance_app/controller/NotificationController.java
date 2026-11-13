package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.NotificationResponse;
import com.pravin.maintenance_app.dto.RegisterPushTokenRequest;
import com.pravin.maintenance_app.dto.UnreadCountResponse;
import com.pravin.maintenance_app.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Notifications", description = "Endpoints for resident notifications and push token registration")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get resident notifications", description = "Retrieves all notifications (broadcasts and targeted) for the authenticated resident.")
    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        List<NotificationResponse> notifications = notificationService.getMyNotifications();
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get unread notifications count", description = "Retrieves the count of unread notifications for the authenticated resident.")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        UnreadCountResponse response = notificationService.getUnreadCount();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read for the authenticated resident.")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read for the authenticated resident.")
    @PatchMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @Operation(summary = "Register device push token", description = "Associates an Expo push token with the authenticated resident for push delivery.")
    @PostMapping("/push-token")
    public ResponseEntity<Map<String, String>> registerPushToken(@Valid @RequestBody RegisterPushTokenRequest request) {
        notificationService.registerPushToken(request);
        return ResponseEntity.ok(Map.of("message", "Push token registered successfully"));
    }

    @Operation(summary = "Remove device push token", description = "Removes a push token upon user logout.")
    @DeleteMapping("/push-token")
    public ResponseEntity<Map<String, String>> removePushToken(@RequestParam String token) {
        notificationService.removePushToken(token);
        return ResponseEntity.ok(Map.of("message", "Push token removed successfully"));
    }
}
