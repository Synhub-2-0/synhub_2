package com.hub.synhub_backend.subscriptions;

import static com.hub.synhub_backend.groups.GroupFixtures.group;
import static com.hub.synhub_backend.groups.GroupFixtures.user;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.subscriptions.application.internal.queryservices.SubscriptionQueryServiceImpl;
import com.hub.synhub_backend.subscriptions.domain.model.aggregates.Subscription;
import com.hub.synhub_backend.subscriptions.domain.model.queries.GetSubscriptionOverviewQuery;
import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionPlan;
import com.hub.synhub_backend.subscriptions.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** "Mi plan": plan actual y cuántos grupos liderados están activos y bloqueados. */
class SubscriptionQueryServiceImplTest {

    private static final long USER = 1L;

    private SubscriptionRepository repo;
    private GroupsContextFacade groups;
    private SubscriptionQueryServiceImpl service;
    private Group active1, active2, locked;

    @BeforeEach
    void setUp() {
        repo = mock(SubscriptionRepository.class);
        groups = mock(GroupsContextFacade.class);
        service = new SubscriptionQueryServiceImpl(repo, groups);
        var leader = user(USER, "lider");
        active1 = group(10, leader);
        active2 = group(11, leader);
        locked = group(12, leader);
        locked.lock();
        when(groups.fetchLedGroups(USER)).thenReturn(List.of(active1, active2, locked));
    }

    @Test
    void withoutASubscriptionItIsFreeAndFull() {
        when(repo.findByUserId(USER)).thenReturn(Optional.empty());

        var overview = service.handle(new GetSubscriptionOverviewQuery(USER));

        assertEquals(SubscriptionPlan.FREE, overview.plan());
        assertEquals(2, overview.activeLedGroups());
        assertEquals(1, overview.lockedLedGroups());
        assertEquals(2, overview.maxLedGroups());
        assertEquals(0, overview.availableSlots());
    }

    @Test
    void aBiggerPlanLeavesSlotsButDoesNotActivateAnything() {
        when(repo.findByUserId(USER)).thenReturn(Optional.of(new Subscription(USER, SubscriptionPlan.PRO)));

        var overview = service.handle(new GetSubscriptionOverviewQuery(USER));

        assertEquals(5, overview.maxLedGroups());
        assertEquals(3, overview.availableSlots());
        assertEquals(1, overview.lockedLedGroups(), "el líder activa los bloqueados uno a uno");
    }

    @Test
    void anUnlimitedPlanHasNoLimitNorSlotCount() {
        when(repo.findByUserId(USER)).thenReturn(Optional.of(new Subscription(USER, SubscriptionPlan.ULTRA)));

        var overview = service.handle(new GetSubscriptionOverviewQuery(USER));

        assertNull(overview.maxLedGroups());
        assertNull(overview.availableSlots());
    }
}
