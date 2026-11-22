package com.hub.synhub_backend.subscriptions.application.internal.commandservices;

import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.subscriptions.domain.model.aggregates.Subscription;
import com.hub.synhub_backend.subscriptions.domain.model.commands.ChangePlanCommand;
import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionPlan;
import com.hub.synhub_backend.subscriptions.domain.services.SubscriptionCommandService;
import com.hub.synhub_backend.subscriptions.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private final SubscriptionRepository subscriptionRepository;
    private final GroupsContextFacade groupsContextFacade;

    public SubscriptionCommandServiceImpl(
            SubscriptionRepository subscriptionRepository, GroupsContextFacade groupsContextFacade) {
        this.subscriptionRepository = subscriptionRepository;
        this.groupsContextFacade = groupsContextFacade;
    }

    /**
     * El pago es simulado: no se guarda ningún dato de pago. Bajar de plan exige elegir qué grupos siguen activos;
     * si la selección no es válida nada cambia (ni el plan ni los grupos). Subir de plan no activa grupos solos.
     */
    @Override
    @Transactional
    public Subscription handle(ChangePlanCommand command) {
        var plan = SubscriptionPlan.fromString(command.plan());
        this.groupsContextFacade.applyGroupLimit(command.userId(), plan.getMaxLedGroups(), command.activeGroupIds());

        var subscription = this.subscriptionRepository
                .findByUserId(command.userId())
                .orElseGet(() -> new Subscription(command.userId(), plan));
        subscription.changePlan(plan);
        return this.subscriptionRepository.save(subscription);
    }
}
