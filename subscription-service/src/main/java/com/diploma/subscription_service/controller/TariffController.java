package com.diploma.subscription_service.controller;

import com.diploma.subscription_service.dto.TariffResponse;
import com.diploma.subscription_service.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
public class TariffController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<List<TariffResponse>> list() {
        return ResponseEntity.ok(subscriptionService.listActiveTariffs());
    }
}
