package com.hub.synhub_backend.subscriptions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.subscriptions.application.internal.acl.SubscriptionsContextFacadeImpl;
import com.hub.synhub_backend.subscriptions.domain.model.aggregates.Subscription;
import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionPlan;
import com.hub.synhub_backend.subscriptions.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** ACL que subscriptions ofrece a groups: cuántos grupos puede liderar activos un usuario. */
class SubscriptionsContextFacadeImplTest {

    private SubscriptionRepository repo;
    private SubscriptionsContextFacadeImpl facade;

    @BeforeEach
    void setUp() {
        repo = mock(SubscriptionRepository.class);
        facade = new SubscriptionsContextFacadeImpl(repo);
    }

    @Test
    void withoutASubscriptionTheUserIsOnTheFreePlan() {
        when(repo.findByUserId(1L)).thenReturn(Optional.empty());
        assertEquals(2, facade.maxLedGroups(1L));
    }

    @Test
    void eachPlanHasItsOwnLimit() {
        when(repo.findByUserId(1L)).thenReturn(Optional.of(new Subscription(1L, SubscriptionPlan.FREE)));
        when(repo.findByUserId(2L)).thenReturn(Optional.of(new Subscription(2L, SubscriptionPlan.PRO)));
        when(repo.findByUserId(3L)).thenReturn(Optional.of(new Subscription(3L, SubscriptionPlan.ULTRA)));
        assertEquals(2, facade.maxLedGroups(1L));
        assertEquals(5, facade.maxLedGroups(2L));
        assertEquals(SubscriptionPlan.UNLIMITED, facade.maxLedGroups(3L));
    }

    @Test
    void plansParseIgnoringCaseAndExposeTheirSimulatedPrice() {
        assertEquals(SubscriptionPlan.PRO, SubscriptionPlan.fromString("pro"));
        assertThrows(IllegalArgumentException.class, () -> SubscriptionPlan.fromString("gold"));
        assertEquals(0, SubscriptionPlan.FREE.getMonthlyPrice());
        assertEquals(9, SubscriptionPlan.PRO.getMonthlyPrice());
        assertEquals(29, SubscriptionPlan.ULTRA.getMonthlyPrice());
        assertTrue(SubscriptionPlan.ULTRA.isUnlimited());
        assertFalse(SubscriptionPlan.PRO.isUnlimited());
    }
}
