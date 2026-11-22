package com.hub.synhub_backend.subscriptions.domain.model.valueobjects;

/**
 * Planes del modelo freemium. El límite cuenta los grupos que el usuario lidera y están activos; los precios son
 * simulados (el MVP no cobra).
 */
public enum SubscriptionPlan {
    FREE("Gratis", 2, 0),
    PRO("Pro", 5, 9),
    ULTRA("Ultra", Integer.MAX_VALUE, 29);

    /** Valor de {@link #getMaxLedGroups()} cuando el plan no tiene límite. */
    public static final int UNLIMITED = Integer.MAX_VALUE;

    private final String displayName;
    private final int maxLedGroups;
    private final int monthlyPrice;

    SubscriptionPlan(String displayName, int maxLedGroups, int monthlyPrice) {
        this.displayName = displayName;
        this.maxLedGroups = maxLedGroups;
        this.monthlyPrice = monthlyPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLedGroups() {
        return maxLedGroups;
    }

    public int getMonthlyPrice() {
        return monthlyPrice;
    }

    public boolean isUnlimited() {
        return maxLedGroups == UNLIMITED;
    }

    public static SubscriptionPlan fromString(String plan) {
        for (var candidate : values()) {
            if (candidate.name().equalsIgnoreCase(plan)) return candidate;
        }
        throw new IllegalArgumentException("Unknown plan: " + plan);
    }
}
