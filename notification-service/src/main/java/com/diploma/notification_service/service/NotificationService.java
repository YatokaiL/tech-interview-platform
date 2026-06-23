package com.diploma.notification_service.service;

import com.diploma.notification_service.client.UserClient;
import com.diploma.notification_service.entity.Notification;
import com.diploma.notification_service.repository.NotificationRepository;
import com.diploma.notification_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final UserClient userClient;
    private final EmailSender emailSender;

    /**
     * Creates an in-app notification for a recipient and (best-effort) emails them.
     * Resolves the recipient email from user-service when not provided.
     */
    @Transactional
    public void notifyUser(UUID recipientUserId, String knownEmail, String type,
                           String title, String message, String link) {
        if (recipientUserId == null) {
            log.debug("No recipient for notification '{}' — skipping", title);
            return;
        }
        String email = knownEmail;
        if (email == null) {
            UserClient.UserInfo u = userClient.fetch(recipientUserId);
            if (u != null) email = u.getEmail();
        }

        Notification n = new Notification();
        n.setRecipientUserId(recipientUserId);
        n.setRecipientEmail(email);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);

        boolean sent = emailSender.send(email, title, message
                + (link != null ? "\n\nСсылка: " + link : ""));
        n.setEmailSent(sent);
        repository.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> list(int limit) {
        int capped = Math.max(1, Math.min(limit, 100));
        return repository.findByRecipientUserIdOrderByCreatedAtDesc(CurrentUser.id(), PageRequest.of(0, capped));
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByRecipientUserIdAndReadFalse(CurrentUser.id());
    }

    @Transactional
    public void markRead(UUID id) {
        repository.findById(id).ifPresent(n -> {
            if (n.getRecipientUserId().equals(CurrentUser.id())) {
                n.setRead(true);
                repository.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead() {
        repository.markAllRead(CurrentUser.id());
    }
}
