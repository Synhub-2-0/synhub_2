package com.hub.synhub_backend.subscriptions.application.internal.acl;

import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionPlan;
import com.hub.synhub_backend.subscriptions.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import com.hub.synhub_backend.subscriptions.interfaces.acl.SubscriptionsContextFacade;
import org.springframework.stereotype.Service;

/**
 * Usa solo el repositorio de su contexto: groups la llama al crear un grupo y subscriptions llama a groups al
 * cambiar de plan, así que pasar por un servicio formaría un ciclo de beans.
 */
@Service
public class SubscriptionsContextFacadeImpl implements SubscriptionsContextFacade {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionsContextFacadeImpl(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public int maxLedGroups(Long userId) {
        return subscriptionRepository
                .findByUserId(userId)
                .map(subscription -> subscription.getPlan().getMaxLedGroups())
                .orElse(SubscriptionPlan.FREE.getMaxLedGroups());
    }
}
