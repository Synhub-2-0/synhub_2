package com.hub.synhub_backend.subscriptions.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.subscriptions.domain.model.aggregates.Subscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserId(Long userId);
}
