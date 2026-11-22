package com.hub.synhub_backend.subscriptions.interfaces.acl;

/** Contrato que subscriptions ofrece a otros contextos. */
public interface SubscriptionsContextFacade {
    /** Cuántos grupos puede liderar activos el usuario según su plan; {@code Integer.MAX_VALUE} si no tiene límite. */
    int maxLedGroups(Long userId);
}
