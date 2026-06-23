package com.diploma.notification_service.repository;

import com.diploma.notification_service.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndReadFalse(UUID recipientUserId);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipientUserId = :userId and n.read = false")
    int markAllRead(@Param("userId") UUID userId);
}
