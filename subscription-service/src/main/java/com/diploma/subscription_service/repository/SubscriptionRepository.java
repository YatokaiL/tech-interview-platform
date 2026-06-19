package com.diploma.subscription_service.repository;

import com.diploma.subscription_service.entity.Subscription;
import com.diploma.subscription_service.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findTopByCompanyIdAndStatusOrderByCreatedAtDesc(UUID companyId, SubscriptionStatus status);

    List<Subscription> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
