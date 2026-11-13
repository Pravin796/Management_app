package com.pravin.maintenance_app.repository;

import com.pravin.maintenance_app.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    @Query("SELECT nr FROM NotificationRecipient nr JOIN FETCH nr.notification n LEFT JOIN FETCH n.targetRoom LEFT JOIN FETCH n.targetUser LEFT JOIN FETCH n.createdBy WHERE nr.user.id = :userId ORDER BY nr.createdAt DESC")
    List<NotificationRecipient> findByUserIdWithNotificationOrderByCreatedAtDesc(@Param("userId") Long userId);

    Optional<NotificationRecipient> findByNotificationIdAndUserId(Long notificationId, Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE NotificationRecipient nr SET nr.isRead = true, nr.readAt = :readAt WHERE nr.user.id = :userId AND nr.isRead = false")
    int markAllAsReadForUser(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}

