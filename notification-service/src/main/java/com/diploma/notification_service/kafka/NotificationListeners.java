package com.diploma.notification_service.kafka;

import com.diploma.notification_service.event.Events;
import com.diploma.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListeners {

    private final NotificationService notificationService;

    @KafkaListener(topics = "user-registered", groupId = "notification-service")
    public void onUserRegistered(Events.UserRegistered e) {
        if (e == null || e.userId() == null) return;
        log.info("user-registered: {}", e.email());
        String name = e.firstName() != null && !e.firstName().isBlank() ? e.firstName() : "коллега";
        notificationService.notifyUser(
                e.userId(),
                e.email(),
                "WELCOME",
                "Добро пожаловать на платформу интервью",
                "Здравствуйте, " + name + "! Ваш аккаунт создан. Теперь вы можете участвовать в технических интервью.",
                null
        );
    }

    @KafkaListener(topics = "session-created", groupId = "notification-service")
    public void onSessionCreated(Events.SessionCreated e) {
        if (e == null || e.candidateId() == null) return;
        log.info("session-created: {}", e.sessionId());
        notificationService.notifyUser(
                e.candidateId(),
                null,
                "SESSION_INVITE",
                "Приглашение на техническое интервью",
                "Вас пригласили на интервью" + (e.language() != null ? " (" + e.language() + ")" : "")
                        + ". Подключитесь к сессии в назначенное время.",
                "/sessions/" + e.sessionId()
        );
    }

    @KafkaListener(topics = "session-completed", groupId = "notification-service")
    public void onSessionCompleted(Events.SessionCompleted e) {
        if (e == null) return;
        log.info("session-completed: {}", e.sessionId());
        notificationService.notifyUser(
                e.candidateId(),
                null,
                "SESSION_COMPLETED",
                "Интервью завершено",
                "Ваше интервью завершено. Ожидайте результаты код-ревью.",
                "/sessions/" + e.sessionId()
        );
    }

    @KafkaListener(topics = "review-completed", groupId = "notification-service")
    public void onReviewCompleted(Events.ReviewCompleted e) {
        if (e == null || e.candidateId() == null) return;
        log.info("review-completed: {}", e.reviewId());
        notificationService.notifyUser(
                e.candidateId(),
                null,
                "REVIEW_READY",
                "Готово код-ревью по вашему интервью",
                "Интервьюер опубликовал ревью по вашей сессии. Откройте страницу ревью, чтобы посмотреть оценки и комментарии.",
                "/sessions/" + e.sessionId() + "/review"
        );
    }

    @KafkaListener(topics = "payment-received", groupId = "notification-service")
    public void onPaymentReceived(Events.PaymentReceived e) {
        if (e == null || e.createdBy() == null) return;
        log.info("payment-received: {}", e.paymentId());
        String amount = e.amountCents() != null
                ? (e.amountCents() / 100) + " " + (e.currency() != null ? e.currency() : "RUB")
                : "";
        notificationService.notifyUser(
                e.createdBy(),
                null,
                "PAYMENT_RECEIVED",
                "Оплата получена — подписка активирована",
                "Оплата " + amount + " прошла успешно. Подписка вашей компании активирована.",
                "/subscription"
        );
    }
}
