package com.diploma.subscription_service.repository;

import com.diploma.subscription_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);
}
