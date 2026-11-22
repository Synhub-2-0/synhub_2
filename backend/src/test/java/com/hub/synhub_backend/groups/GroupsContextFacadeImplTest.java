package com.hub.synhub_backend.groups;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.groups.application.internal.acl.GroupsContextFacadeImpl;
import com.hub.synhub_backend.groups.domain.exceptions.InvalidActiveGroupSelectionException;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** ACL que groups ofrece a subscriptions: grupos liderados y límite por plan. */
class GroupsContextFacadeImplTest {

    private static final long LEADER = 1L;

    private GroupProfileRepository profiles;
    private GroupRepository groups;
    private GroupsContextFacadeImpl facade;
    private User leader;
    private Group g10, g11, g12, others;

    @BeforeEach
    void setUp() {
        profiles = mock(GroupProfileRepository.class);
        groups = mock(GroupRepository.class);
        facade = new GroupsContextFacadeImpl(mock(GroupQueryService.class), profiles, groups);
        leader = user(LEADER, "lider");
        // Se crean desordenados: el orden por id lo pone la fachada.
        g12 = group(12, leader);
        g10 = group(10, leader);
        g11 = group(11, leader);
        others = group(20, user(2, "otro"), leader); // aquí es solo integrante
        when(profiles.findByUser_IdAndRoleInGroup(LEADER, GroupRole.GROUP_LEADER))
                .thenAnswer(inv -> leader.getGroupProfiles().stream()
                        .filter(p -> p.getRoleInGroup() == GroupRole.GROUP_LEADER)
                        .toList());
    }

    @Test
    void ledGroupsAreOnlyTheOnesTheUserLeadsInOrder() {
        assertEquals(List.of(g10, g11, g12), facade.fetchLedGroups(LEADER));
        assertEquals(GroupRole.GROUP_MEMBER, others.getUserProfiles().get(1).getRoleInGroup());
    }

    @Test
    void withinTheLimitNothingIsLocked() {
        facade.applyGroupLimit(LEADER, 3, null);
        assertTrue(facade.fetchLedGroups(LEADER).stream().noneMatch(Group::isLocked));
        verify(groups, never()).save(any());
    }

    @Test
    void anUnlimitedPlanNeverLocksAnything() {
        facade.applyGroupLimit(LEADER, Integer.MAX_VALUE, null);
        assertTrue(facade.fetchLedGroups(LEADER).stream().noneMatch(Group::isLocked));
    }

    @Test
    void aLowerLimitLocksTheGroupsThatAreNotChosen() {
        facade.applyGroupLimit(LEADER, 2, List.of(10L, 12L));

        assertFalse(g10.isLocked());
        assertTrue(g11.isLocked());
        assertFalse(g12.isLocked());
        verify(groups).save(g11);
        verify(groups, never()).save(g10);
    }

    @Test
    void lockedGroupsDoNotCountAsActive() {
        g12.lock();
        facade.applyGroupLimit(LEADER, 2, null); // solo 2 activas: ya cabe
        assertFalse(g10.isLocked());
        assertFalse(g11.isLocked());
        verify(groups, never()).save(any());
    }

    @Test
    void theSelectionMustBeExactlyTheLimitAndAmongTheActiveGroups() {
        for (var bad : List.of(
                List.<Long>of(), // ninguna
                List.of(10L), // menos del límite
                List.of(10L, 11L, 12L), // más del límite
                List.of(10L, 10L), // repetida
                List.of(10L, 20L), // 20: solo es integrante, no lo lidera
                List.of(10L, 99L))) { // no existe
            assertThrows(
                    InvalidActiveGroupSelectionException.class, () -> facade.applyGroupLimit(LEADER, 2, bad), "" + bad);
        }
        assertThrows(InvalidActiveGroupSelectionException.class, () -> facade.applyGroupLimit(LEADER, 2, null));
        assertTrue(facade.fetchLedGroups(LEADER).stream().noneMatch(Group::isLocked), "nada se bloquea a medias");
        verify(groups, never()).save(any());
    }
}
