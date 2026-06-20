package com.diploma.subscription_service.controller;

import com.diploma.subscription_service.dto.SubscriptionRequest;
import com.diploma.subscription_service.dto.SubscriptionResponse;
import com.diploma.subscription_service.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<SubscriptionResponse> current() {
        SubscriptionResponse body = subscriptionService.getCurrent();
        return body == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(body);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<List<SubscriptionResponse>> list() {
        return ResponseEntity.ok(subscriptionService.listForCompany());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<SubscriptionResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.cancel(id));
    }
}
