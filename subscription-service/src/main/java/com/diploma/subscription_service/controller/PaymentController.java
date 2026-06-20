package com.diploma.subscription_service.controller;

import com.diploma.subscription_service.dto.SubscriptionResponse;
import com.diploma.subscription_service.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Mock payment-gateway webhook: a separate process (or the demo UI) calls this
 * with the gateway reference of a pending payment to mark it succeeded/failed.
 * Open endpoint — no JWT required, matches how real gateways post webhooks.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/webhook")
    public ResponseEntity<SubscriptionResponse> webhook(@RequestBody WebhookPayload payload) {
        boolean success = payload.success() == null || payload.success();
        return ResponseEntity.ok(subscriptionService.confirmPayment(payload.paymentId(), success));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<SubscriptionResponse> confirm(@org.springframework.web.bind.annotation.PathVariable UUID id) {
        // Convenience endpoint that lets the demo UI "pay" without simulating the gateway round-trip.
        return ResponseEntity.ok(subscriptionService.confirmPayment(id, true));
    }

    public record WebhookPayload(UUID paymentId, Boolean success) {}
}
