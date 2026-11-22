package com.hub.synhub_backend.subscriptions.interfaces.rest.resources;

import java.util.List;

public record SubscriptionResource(
        String plan,
        String planName,
        int monthlyPrice,
        Integer maxLedGroups,
        int activeLedGroups,
        int lockedLedGroups,
        Integer availableSlots,
        List<PlanResource> plans) {}
