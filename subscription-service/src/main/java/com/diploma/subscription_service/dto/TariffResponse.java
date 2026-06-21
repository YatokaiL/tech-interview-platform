package com.diploma.subscription_service.dto;

import com.diploma.subscription_service.entity.Tariff;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record TariffResponse(
        UUID id,
        String code,
        String name,
        String description,
        Integer priceCents,
        String currency,
        Integer maxSessionsPerMonth,
        List<String> features,
        Integer sortOrder,
        Boolean active
) {
    public static TariffResponse from(Tariff t) {
        List<String> features = t.getFeatures() == null || t.getFeatures().isBlank()
                ? List.of()
                : Arrays.stream(t.getFeatures().split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new TariffResponse(
                t.getId(),
                t.getCode(),
                t.getName(),
                t.getDescription(),
                t.getPriceCents(),
                t.getCurrency(),
                t.getMaxSessionsPerMonth(),
                features,
                t.getSortOrder(),
                t.getActive()
        );
    }
}
