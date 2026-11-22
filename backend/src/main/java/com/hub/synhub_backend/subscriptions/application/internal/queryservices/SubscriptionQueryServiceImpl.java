package com.hub.synhub_backend.subscriptions.application.internal.queryservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.subscriptions.domain.model.queries.GetSubscriptionOverviewQuery;
import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionOverview;
import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionPlan;
import com.hub.synhub_backend.subscriptions.domain.services.SubscriptionQueryService;
import com.hub.synhub_backend.subscriptions.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;
    private final GroupsContextFacade groupsContextFacade;

    public SubscriptionQueryServiceImpl(
            SubscriptionRepository subscriptionRepository, GroupsContextFacade groupsContextFacade) {
        this.subscriptionRepository = subscriptionRepository;
        this.groupsContextFacade = groupsContextFacade;
    }

    @Override
    public SubscriptionOverview handle(GetSubscriptionOverviewQuery query) {
        var plan = this.subscriptionRepository
                .findByUserId(query.userId())
                .map(subscription -> subscription.getPlan())
                .orElse(SubscriptionPlan.FREE);
        var led = this.groupsContextFacade.fetchLedGroups(query.userId());
        int locked = (int) led.stream().filter(Group::isLocked).count();
        return new SubscriptionOverview(plan, led.size() - locked, locked);
    }
}
