package com.diploma.subscription_service.service;

import com.diploma.subscription_service.dto.PaymentReceivedEvent;
import com.diploma.subscription_service.dto.PaymentResponse;
import com.diploma.subscription_service.dto.SubscriptionRequest;
import com.diploma.subscription_service.dto.SubscriptionResponse;
import com.diploma.subscription_service.dto.TariffResponse;
import com.diploma.subscription_service.entity.Payment;
import com.diploma.subscription_service.entity.PaymentStatus;
import com.diploma.subscription_service.entity.Subscription;
import com.diploma.subscription_service.entity.SubscriptionStatus;
import com.diploma.subscription_service.entity.Tariff;
import com.diploma.subscription_service.exception.ForbiddenException;
import com.diploma.subscription_service.exception.NotFoundException;
import com.diploma.subscription_service.exception.ValidationException;
import com.diploma.subscription_service.kafka.PaymentEventPublisher;
import com.diploma.subscription_service.repository.PaymentRepository;
import com.diploma.subscription_service.repository.SubscriptionRepository;
import com.diploma.subscription_service.repository.TariffRepository;
import com.diploma.subscription_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final TariffRepository tariffRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    // ---------- TARIFFS ----------

    @Transactional(readOnly = true)
    public List<TariffResponse> listActiveTariffs() {
        return tariffRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(TariffResponse::from)
                .toList();
    }

    // ---------- SUBSCRIPTIONS ----------

    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrent() {
        UUID companyId = requireCompany();
        // Pick the latest ACTIVE first, otherwise the latest PENDING
        Optional<Subscription> sub = subscriptionRepository
                .findTopByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, SubscriptionStatus.ACTIVE);
        if (sub.isEmpty()) {
            sub = subscriptionRepository.findTopByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, SubscriptionStatus.PENDING);
        }
        return sub.map(this::toResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listForCompany() {
        UUID companyId = requireCompany();
        return subscriptionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        UUID companyId = requireCompany();
        Tariff tariff = tariffRepository.findById(request.tariffId())
                .orElseThrow(() -> new NotFoundException("Tariff", request.tariffId()));
        if (!Boolean.TRUE.equals(tariff.getActive())) {
            throw new ValidationException("Tariff is not active");
        }

        int months = request.periodMonths() == null ? 1 : request.periodMonths();
        Subscription sub = new Subscription();
        sub.setCompanyId(companyId);
        sub.setTariffId(tariff.getId());
        sub.setStatus(SubscriptionStatus.PENDING);
        sub.setPeriodMonths(months);
        sub.setAutoRenew(Boolean.TRUE.equals(request.autoRenew()));
        sub.setCreatedBy(CurrentUser.id());
        sub = subscriptionRepository.save(sub);

        Payment payment = new Payment();
        payment.setSubscriptionId(sub.getId());
        payment.setAmountCents(tariff.getPriceCents() * months);
        payment.setCurrency(tariff.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayRef("mock-" + UUID.randomUUID());
        paymentRepository.save(payment);

        log.info("Created PENDING subscription {} for company {} on tariff {} (payment {})",
                sub.getId(), companyId, tariff.getCode(), payment.getId());
        return toResponse(sub);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription", subscriptionId));
        ensureOwnerOrAdmin(sub);
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) return toResponse(sub);
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setAutoRenew(false);
        return toResponse(sub);
    }

    // ---------- PAYMENTS / WEBHOOK ----------

    @Transactional
    public SubscriptionResponse confirmPayment(UUID paymentId, boolean success) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            // Idempotent — return current state.
            Subscription existing = subscriptionRepository.findById(payment.getSubscriptionId())
                    .orElseThrow(() -> new NotFoundException("Subscription", payment.getSubscriptionId()));
            return toResponse(existing);
        }

        Subscription sub = subscriptionRepository.findById(payment.getSubscriptionId())
                .orElseThrow(() -> new NotFoundException("Subscription", payment.getSubscriptionId()));

        Instant now = Instant.now();
        if (success) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setPaidAt(now);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setStartedAt(now);
            sub.setEndsAt(now.plus(30L * sub.getPeriodMonths(), ChronoUnit.DAYS));

            Tariff tariff = tariffRepository.findById(sub.getTariffId())
                    .orElseThrow(() -> new NotFoundException("Tariff", sub.getTariffId()));
            eventPublisher.publishPaymentReceived(new PaymentReceivedEvent(
                    payment.getId(),
                    sub.getId(),
                    sub.getCompanyId(),
                    tariff.getId(),
                    sub.getCreatedBy(),
                    payment.getAmountCents(),
                    payment.getCurrency(),
                    now
            ));
            log.info("Payment {} SUCCEEDED for subscription {} (company {})",
                    payment.getId(), sub.getId(), sub.getCompanyId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            log.info("Payment {} FAILED for subscription {}", payment.getId(), sub.getId());
        }
        return toResponse(sub);
    }

    // ---------- helpers ----------

    private UUID requireCompany() {
        UUID companyId = CurrentUser.companyId();
        if (companyId == null) {
            throw new ForbiddenException("Current user has no company; subscriptions are scoped to a company");
        }
        return companyId;
    }

    private void ensureOwnerOrAdmin(Subscription sub) {
        if (CurrentUser.isAdmin()) return;
        UUID userCompany = CurrentUser.companyId();
        if (userCompany == null || !userCompany.equals(sub.getCompanyId())) {
            throw new ForbiddenException("You can only manage subscriptions of your own company");
        }
    }

    private SubscriptionResponse toResponse(Subscription sub) {
        Tariff tariff = tariffRepository.findById(sub.getTariffId()).orElse(null);
        TariffResponse tariffResponse = tariff == null ? null : TariffResponse.from(tariff);
        List<PaymentResponse> payments = paymentRepository.findBySubscriptionIdOrderByCreatedAtDesc(sub.getId()).stream()
                .map(PaymentResponse::from)
                .toList();
        return SubscriptionResponse.from(sub, tariffResponse, payments);
    }
}
