package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.NotificationCategory;
import com.pravin.maintenance_app.ENUM.NotificationTargetType;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.dto.CreateNotificationRequest;
import com.pravin.maintenance_app.dto.NotificationResponse;
import com.pravin.maintenance_app.dto.RegisterPushTokenRequest;
import com.pravin.maintenance_app.dto.UnreadCountResponse;
import com.pravin.maintenance_app.entity.*;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.repository.*;
import com.pravin.maintenance_app.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final UserPushTokenRepository userPushTokenRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ExpoPushService expoPushService;
    private final CurrentUserService currentUserService;

    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        User admin = currentUserService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only administrators can send notifications");
        }

        NotificationCategory category = request.getCategory() != null ? request.getCategory() : NotificationCategory.ANNOUNCEMENT;
        NotificationTargetType targetType = request.getTargetType() != null ? request.getTargetType() : NotificationTargetType.ALL_RESIDENTS;

        Notification.NotificationBuilder builder = Notification.builder()
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .category(category)
                .targetType(targetType)
                .createdBy(admin)
                .createdAt(LocalDateTime.now());

        List<User> targetRecipients = new ArrayList<>();
        List<String> pushTokens = new ArrayList<>();

        if (targetType == NotificationTargetType.SPECIFIC_USER) {
            User targetUser = null;
            if (request.getTargetUserId() != null) {
                targetUser = userRepository.findById(request.getTargetUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + request.getTargetUserId()));
            } else if (request.getTargetRoomId() != null) {
                targetUser = userRepository.findByRoomId(request.getTargetRoomId())
                        .orElseThrow(() -> new ResourceNotFoundException("No registered resident found for room id: " + request.getTargetRoomId()));
            } else {
                throw new BusinessValidationException("Either targetUserId or targetRoomId must be specified for SPECIFIC_USER notification");
            }

            builder.targetUser(targetUser);
            builder.targetRoom(targetUser.getRoom());
            targetRecipients.add(targetUser);
            pushTokens.addAll(userPushTokenRepository.findTokensByUserIds(List.of(targetUser.getId())));
        } else {
            // ALL_RESIDENTS
            List<User> residents = userRepository.findByRole(Role.USER);
            targetRecipients.addAll(residents);
            pushTokens.addAll(userPushTokenRepository.findAllTokens());
        }

        Notification savedNotification = notificationRepository.save(builder.build());

        // Create recipient rows
        LocalDateTime now = LocalDateTime.now();
        List<NotificationRecipient> recipients = targetRecipients.stream()
                .map(u -> NotificationRecipient.builder()
                        .notification(savedNotification)
                        .user(u)
                        .isRead(false)
                        .createdAt(now)
                        .build())
                .collect(Collectors.toList());

        if (!recipients.isEmpty()) {
            notificationRecipientRepository.saveAll(recipients);
        }

        // Dispatch push notifications to devices asynchronously
        Map<String, Object> pushData = new HashMap<>();
        pushData.put("notificationId", savedNotification.getId());
        pushData.put("category", savedNotification.getCategory().name());
        pushData.put("targetType", savedNotification.getTargetType().name());

        expoPushService.sendPushNotificationsAsync(pushTokens, savedNotification.getTitle(), savedNotification.getMessage(), pushData);

        return mapToResponse(savedNotification, null, false, null);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        User currentUser = currentUserService.getCurrentUser();
        List<NotificationRecipient> recipients = notificationRecipientRepository
                .findByUserIdWithNotificationOrderByCreatedAtDesc(currentUser.getId());

        return recipients.stream()
                .map(nr -> mapToResponse(nr.getNotification(), nr.getId(), nr.isRead(), nr.getReadAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        User currentUser = currentUserService.getCurrentUser();
        long count = notificationRecipientRepository.countByUserIdAndIsReadFalse(currentUser.getId());
        return new UnreadCountResponse(count);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<NotificationRecipient> opt = notificationRecipientRepository
                .findByNotificationIdAndUserId(notificationId, currentUser.getId());

        if (opt.isPresent()) {
            NotificationRecipient nr = opt.get();
            if (!nr.isRead()) {
                nr.setRead(true);
                nr.setReadAt(LocalDateTime.now());
                notificationRecipientRepository.save(nr);
            }
        }
    }

    @Transactional
    public void markAllAsRead() {
        User currentUser = currentUserService.getCurrentUser();
        notificationRecipientRepository.markAllAsReadForUser(currentUser.getId(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllAdminNotifications() {
        User admin = currentUserService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only administrators can view notification history");
        }

        List<Notification> notifications = notificationRepository.findAllByOrderByCreatedAtDesc();
        return notifications.stream()
                .map(n -> mapToResponse(n, n.getId(), false, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public void registerPushToken(RegisterPushTokenRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        String token = request.getPushToken().trim();

        Optional<UserPushToken> existingOpt = userPushTokenRepository.findByPushToken(token);
        LocalDateTime now = LocalDateTime.now();

        if (existingOpt.isPresent()) {
            UserPushToken existing = existingOpt.get();
            existing.setUser(currentUser);
            if (request.getDeviceType() != null) {
                existing.setDeviceType(request.getDeviceType());
            }
            existing.setUpdatedAt(now);
            userPushTokenRepository.save(existing);
        } else {
            UserPushToken newToken = UserPushToken.builder()
                    .user(currentUser)
                    .pushToken(token)
                    .deviceType(request.getDeviceType())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            userPushTokenRepository.save(newToken);
        }
    }

    @Transactional
    public void removePushToken(String token) {
        if (token != null && !token.isBlank()) {
            userPushTokenRepository.deleteByPushToken(token.trim());
        }
    }

    private NotificationResponse mapToResponse(Notification n, Long recipientId, boolean isRead, LocalDateTime readAt) {
        return NotificationResponse.builder()
                .id(recipientId != null ? recipientId : n.getId())
                .notificationId(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .category(n.getCategory())
                .targetType(n.getTargetType())
                .targetUserId(n.getTargetUser() != null ? n.getTargetUser().getId() : null)
                .targetUserName(n.getTargetUser() != null ? n.getTargetUser().getName() : null)
                .targetRoomId(n.getTargetRoom() != null ? n.getTargetRoom().getId() : null)
                .targetRoomNumber(n.getTargetRoom() != null ? n.getTargetRoom().getRoomNumber() : null)
                .createdByName(n.getCreatedBy() != null ? n.getCreatedBy().getName() : "Admin")
                .isRead(isRead)
                .readAt(readAt)
                .createdAt(n.getCreatedAt())
                .build();
    }
}
