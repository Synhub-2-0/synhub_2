package com.hub.synhub_backend.subscriptions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.groups.domain.exceptions.InvalidActiveGroupSelectionException;
import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.subscriptions.application.internal.commandservices.SubscriptionCommandServiceImpl;
import com.hub.synhub_backend.subscriptions.domain.model.aggregates.Subscription;
import com.hub.synhub_backend.subscriptions.domain.model.commands.ChangePlanCommand;
import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionPlan;
import com.hub.synhub_backend.subscriptions.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Cambiar de plan: guarda el plan y aplica el límite a los grupos que el usuario lidera. */
class SubscriptionCommandServiceImplTest {

    private static final long USER = 1L;

    private SubscriptionRepository repo;
    private GroupsContextFacade groups;
    private SubscriptionCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(SubscriptionRepository.class);
        groups = mock(GroupsContextFacade.class);
        service = new SubscriptionCommandServiceImpl(repo, groups);
        when(repo.findByUserId(USER)).thenReturn(Optional.empty());
        when(repo.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void firstPlanChangeCreatesTheSubscriptionAndAppliesTheLimit() {
        var saved = service.handle(new ChangePlanCommand(USER, "PRO", null));

        assertEquals(SubscriptionPlan.PRO, saved.getPlan());
        assertEquals(USER, saved.getUserId());
        verify(groups).applyGroupLimit(USER, 5, null);
    }

    @Test
    void anExistingSubscriptionIsUpdatedNotDuplicated() {
        var current = new Subscription(USER, SubscriptionPlan.PRO);
        when(repo.findByUserId(USER)).thenReturn(Optional.of(current));

        service.handle(new ChangePlanCommand(USER, "ultra", null));

        var captor = ArgumentCaptor.forClass(Subscription.class);
        verify(repo).save(captor.capture());
        assertSame(current, captor.getValue());
        assertEquals(SubscriptionPlan.ULTRA, current.getPlan());
        verify(groups).applyGroupLimit(USER, SubscriptionPlan.UNLIMITED, null);
    }

    @Test
    void lowerPlanPassesTheChosenActiveGroups() {
        service.handle(new ChangePlanCommand(USER, "FREE", List.of(10L, 12L)));
        verify(groups).applyGroupLimit(USER, 2, List.of(10L, 12L));
    }

    @Test
    void anInvalidSelectionLeavesThePlanUntouched() {
        doThrow(new InvalidActiveGroupSelectionException(2)).when(groups).applyGroupLimit(USER, 2, List.of(10L));

        assertThrows(
                InvalidActiveGroupSelectionException.class,
                () -> service.handle(new ChangePlanCommand(USER, "FREE", List.of(10L))));
        verify(repo, never()).save(any());
    }

    @Test
    void anUnknownPlanIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.handle(new ChangePlanCommand(USER, "GOLD", null)));
        verifyNoInteractions(groups);
        verify(repo, never()).save(any());
    }
}
