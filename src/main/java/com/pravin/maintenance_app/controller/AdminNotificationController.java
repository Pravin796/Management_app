package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.CreateNotificationRequest;
import com.pravin.maintenance_app.dto.NotificationResponse;
import com.pravin.maintenance_app.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Notifications", description = "Endpoints for administrators to push announcements and targeted notifications")
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Broadcast or send targeted notification", description = "Allows administrators to push an announcement to all residents or a specific user/room, and dispatches push notifications to active mobile devices.")
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all admin notifications", description = "Retrieves the history of all notifications and announcements sent by administrators.")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllAdminNotifications() {
        List<NotificationResponse> list = notificationService.getAllAdminNotifications();
        return ResponseEntity.ok(list);
    }
}

