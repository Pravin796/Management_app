package com.pravin.maintenance_app.repository;

import com.pravin.maintenance_app.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);
}

