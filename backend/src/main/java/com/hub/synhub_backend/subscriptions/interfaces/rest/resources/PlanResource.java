package com.hub.synhub_backend.subscriptions.interfaces.rest.resources;

/** Un plan del catálogo; {@code maxLedGroups} es null cuando no tiene límite. */
public record PlanResource(String plan, String name, int monthlyPrice, Integer maxLedGroups) {}
