package com.hub.synhub_backend.subscriptions.domain.model.valueobjects;

/** "Mi plan": el plan y cuántos de los grupos que el usuario lidera están activos y bloqueados. */
public record SubscriptionOverview(SubscriptionPlan plan, int activeLedGroups, int lockedLedGroups) {

    /** Límite de grupos activos; {@code null} si el plan no tiene. */
    public Integer maxLedGroups() {
        return plan.isUnlimited() ? null : plan.getMaxLedGroups();
    }

    /** Cupos libres para activar grupos; {@code null} si el plan no tiene límite. */
    public Integer availableSlots() {
        return plan.isUnlimited() ? null : Math.max(0, plan.getMaxLedGroups() - activeLedGroups);
    }
}
