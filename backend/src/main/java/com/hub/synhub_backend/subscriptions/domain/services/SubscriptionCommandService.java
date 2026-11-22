package com.hub.synhub_backend.subscriptions.domain.services;

import com.hub.synhub_backend.subscriptions.domain.model.aggregates.Subscription;
import com.hub.synhub_backend.subscriptions.domain.model.commands.ChangePlanCommand;

public interface SubscriptionCommandService {
    Subscription handle(ChangePlanCommand command);
}
