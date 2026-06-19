package com.diploma.subscription_service.repository;

import com.diploma.subscription_service.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TariffRepository extends JpaRepository<Tariff, UUID> {

    List<Tariff> findByActiveTrueOrderBySortOrderAsc();

    Optional<Tariff> findByCode(String code);
}
