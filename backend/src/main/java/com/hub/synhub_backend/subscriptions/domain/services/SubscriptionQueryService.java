package com.hub.synhub_backend.subscriptions.domain.services;

import com.hub.synhub_backend.subscriptions.domain.model.queries.GetSubscriptionOverviewQuery;
import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionOverview;

public interface SubscriptionQueryService {
    SubscriptionOverview handle(GetSubscriptionOverviewQuery query);
}
