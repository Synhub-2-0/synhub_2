package com.hub.synhub_backend.subscriptions.interfaces.rest.transform;

import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionOverview;
import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionPlan;
import com.hub.synhub_backend.subscriptions.interfaces.rest.resources.PlanResource;
import com.hub.synhub_backend.subscriptions.interfaces.rest.resources.SubscriptionResource;
import java.util.Arrays;

public class SubscriptionResourceFromOverviewAssembler {
    public static SubscriptionResource toResourceFromOverview(SubscriptionOverview overview) {
        var plan = overview.plan();
        return new SubscriptionResource(
                plan.name(),
                plan.getDisplayName(),
                plan.getMonthlyPrice(),
                overview.maxLedGroups(),
                overview.activeLedGroups(),
                overview.lockedLedGroups(),
                overview.availableSlots(),
                Arrays.stream(SubscriptionPlan.values())
                        .map(SubscriptionResourceFromOverviewAssembler::toPlanResource)
                        .toList());
    }

    private static PlanResource toPlanResource(SubscriptionPlan plan) {
        return new PlanResource(
                plan.name(),
                plan.getDisplayName(),
                plan.getMonthlyPrice(),
                plan.isUnlimited() ? null : plan.getMaxLedGroups());
    }
}
