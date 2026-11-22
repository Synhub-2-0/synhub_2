package com.hub.synhub_backend.groups;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.groups.application.internal.commandservices.GroupCommandServiceImpl;
import com.hub.synhub_backend.groups.domain.exceptions.GroupAlreadyActiveException;
import com.hub.synhub_backend.groups.domain.exceptions.GroupLockedException;
import com.hub.synhub_backend.groups.domain.exceptions.LeaderCannotBeRemovedException;
import com.hub.synhub_backend.groups.domain.exceptions.NoFreeGroupSlotException;
import com.hub.synhub_backend.groups.domain.exceptions.UserAlreadyInGroupException;
import com.hub.synhub_backend.groups.domain.exceptions.UserNotInGroupException;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.commands.*;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.interfaces.acl.IamContextFacade;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.subscriptions.interfaces.acl.SubscriptionsContextFacade;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** Crear, editar, borrar grupos y quitar integrantes. */
class GroupCommandServiceImplTest {

    private GroupRepository groups;
    private IamContextFacade users;
    private GroupProfileRepository profiles;
    private TasksContextFacade tasks;
    private InvitationRepository invitations;
    private SubscriptionsContextFacade subscriptions;
    private GroupCommandServiceImpl service;
    private User leader, member;
    private Group group;

    @BeforeEach
    void setUp() {
        groups = mock(GroupRepository.class);
        users = mock(IamContextFacade.class);
        profiles = mock(GroupProfileRepository.class);
        tasks = mock(TasksContextFacade.class);
        invitations = mock(InvitationRepository.class);
        subscriptions = mock(SubscriptionsContextFacade.class);
        when(subscriptions.maxLedGroups(any())).thenReturn(2);
        service = new GroupCommandServiceImpl(groups, users, profiles, tasks, invitations, subscriptions);
        leader = user(1, "lider");
        member = user(2, "ana");
        group = group(10, leader, member);
        when(groups.findById(10L)).thenReturn(Optional.of(group));
        when(groups.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        when(users.fetchUserById(1L)).thenReturn(Optional.of(leader));
        when(users.fetchUserById(2L)).thenReturn(Optional.of(member));
    }

    // ---- Un grupo bloqueado es de solo lectura
    @Test
    void aLockedGroupCannotBeEdited() {
        group.lock();
        assertThrows(
                GroupLockedException.class,
                () -> service.handle(new UpdateGroupCommand(1L, 10L, "Renombrado", "", "")));
        assertEquals("Equipo", group.getName());
    }

    @Test
    void permissionsAreCheckedBeforeTheLock() {
        group.lock();
        assertThrows(AccessDeniedException.class, () -> service.handle(new UpdateGroupCommand(2L, 10L, "X", "", "")));
    }

    @Test
    void nobodyJoinsALockedGroupByCode() {
        group.lock();
        when(groups.findByCode(group.getCode())).thenReturn(Optional.of(group));
        var newcomer = user(3, "luis");
        when(users.fetchUserById(3L)).thenReturn(Optional.of(newcomer));

        assertThrows(
                GroupLockedException.class,
                () -> service.handle(new JoinGroupByCodeCommand(group.getCode().code(), 3L)));
        assertEquals(2, group.getMemberCount());
    }

    @Test
    void theLeaderCannotRemoveMembersFromALockedGroupButTheyCanLeave() {
        group.lock();
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.of(group.getUserProfiles().get(1)));

        assertThrows(
                GroupLockedException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(1L, 2L, 10L)));
        assertEquals(2, group.getMemberCount());

        service.handle(new RemoveMemberUserFromGroupCommand(2L, 2L, 10L)); // salir del grupo sigue permitido
        assertEquals(1, group.getMemberCount());
    }

    @Test
    void aLockedGroupCanStillBeDeletedByItsLeader() {
        group.lock();
        service.handle(new DeleteGroupCommand(1L, 10L));
        verify(groups).delete(group);
    }

    // ---- Crear: por encima del límite del plan el grupo nace bloqueado
    private void leaderAlreadyLeads(Group... led) {
        // setUp ya hizo que el líder lidere el grupo 10; los demás se agregan.
        when(profiles.findByUser_IdAndRoleInGroup(1L, GroupRole.GROUP_LEADER))
                .thenAnswer(inv -> leader.getGroupProfiles().stream()
                        .filter(p -> p.getRoleInGroup() == GroupRole.GROUP_LEADER)
                        .toList());
        for (var g : led) join(g, leader, GroupRole.GROUP_LEADER);
    }

    private Group created() {
        return service.handle(new CreateGroupCommand("Nuevo", "", "desc", 1L));
    }

    @Test
    void aGroupWithinThePlanLimitIsCreatedActive() {
        leaderAlreadyLeads(); // lidera solo el grupo 10: 1 de 2
        assertFalse(created().isLocked());
    }

    @Test
    void aGroupAboveThePlanLimitIsCreatedLocked() {
        var other = group(11, user(9, "x"));
        leaderAlreadyLeads(other); // 10 y 11: 2 de 2
        assertTrue(created().isLocked());
    }

    @Test
    void lockedGroupsDoNotCountTowardsTheLimitWhenCreating() {
        var other = group(11, user(9, "x"));
        other.lock();
        leaderAlreadyLeads(other); // 10 activo y 11 bloqueado: 1 de 2
        assertFalse(created().isLocked());
    }

    @Test
    void groupsWhereTheUserIsOnlyAMemberDoNotCount() {
        var foreign = group(11, user(9, "otro"));
        var foreign2 = group(12, user(8, "otro2"));
        leaderAlreadyLeads();
        join(foreign, leader, GroupRole.GROUP_MEMBER);
        join(foreign2, leader, GroupRole.GROUP_MEMBER);
        assertFalse(created().isLocked());
    }

    @Test
    void anUnlimitedPlanNeverLocksANewGroup() {
        when(subscriptions.maxLedGroups(1L)).thenReturn(Integer.MAX_VALUE);
        leaderAlreadyLeads(group(11, user(9, "x")), group(12, user(8, "y")), group(13, user(7, "z")));
        assertFalse(created().isLocked());
    }

    // ---- Activar un grupo bloqueado
    @Test
    void theLeaderActivatesALockedGroupWhenThereIsASlot() {
        group.lock();
        when(profiles.findByUser_IdAndRoleInGroup(1L, GroupRole.GROUP_LEADER))
                .thenAnswer(inv -> leader.getGroupProfiles().stream()
                        .filter(p -> p.getRoleInGroup() == GroupRole.GROUP_LEADER)
                        .toList()); // solo el 10, bloqueado: 0 de 2

        var activated = service.handle(new ActivateGroupCommand(1L, 10L));

        assertFalse(activated.isLocked());
        verify(groups).save(group);
    }

    @Test
    void activatingNeedsAFreeSlot() {
        group.lock();
        var g11 = group(11, user(9, "x"));
        var g12 = group(12, user(8, "y"));
        join(g11, leader, GroupRole.GROUP_LEADER);
        join(g12, leader, GroupRole.GROUP_LEADER);
        when(profiles.findByUser_IdAndRoleInGroup(1L, GroupRole.GROUP_LEADER))
                .thenAnswer(inv -> leader.getGroupProfiles().stream()
                        .filter(p -> p.getRoleInGroup() == GroupRole.GROUP_LEADER)
                        .toList()); // 11 y 12 activos: 2 de 2

        assertThrows(NoFreeGroupSlotException.class, () -> service.handle(new ActivateGroupCommand(1L, 10L)));
        assertTrue(group.isLocked());
        verify(groups, never()).save(any(Group.class));
    }

    @Test
    void anActiveGroupCannotBeActivatedAgain() {
        assertThrows(GroupAlreadyActiveException.class, () -> service.handle(new ActivateGroupCommand(1L, 10L)));
    }

    @Test
    void onlyTheLeaderActivates() {
        group.lock();
        assertThrows(AccessDeniedException.class, () -> service.handle(new ActivateGroupCommand(2L, 10L)));
        assertThrows(AccessDeniedException.class, () -> service.handle(new ActivateGroupCommand(99L, 10L)));
        assertTrue(group.isLocked());
    }

    @Test
    void activatingAnUnknownGroupIs404() {
        when(groups.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new ActivateGroupCommand(1L, 99L)));
    }

    // ---- Crear
    @Test
    void creatorBecomesLeaderAndCodeIsRegeneratedOnCollision() {
        when(groups.existsByCode(any(GroupCode.class))).thenReturn(true, false);

        var created = service.handle(new CreateGroupCommand("Nuevo", "", "desc", 1L));

        assertSame(leader, created.getLeader());
        assertEquals(1, created.getMemberCount());
        assertTrue(created.getCode().code().matches("[0-9A-Z]{9}"));
        verify(groups, times(2)).existsByCode(any(GroupCode.class));
        verify(groups).save(created);
    }

    @Test
    void createWithUnknownLeaderIs404() {
        when(users.fetchUserById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new CreateGroupCommand("G", "", "d", 99L)));
        verify(groups, never()).save(any());
    }

    // ---- Editar
    @Test
    void leaderUpdatesGroup() {
        var updated = service.handle(new UpdateGroupCommand(1L, 10L, "Renombrado", "", ""));
        assertEquals("Renombrado", updated.getName());
    }

    @Test
    void memberCannotUpdateGroup() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new UpdateGroupCommand(2L, 10L, "X", "", "")));
        verify(groups, never()).save(any());
    }

    // ---- Borrar
    @Test
    void leaderDeletesGroup() {
        service.handle(new DeleteGroupCommand(1L, 10L));
        // AUDIT BUG-02: primero las tareas (y sus requests), después el grupo
        var order = inOrder(tasks, groups);
        order.verify(tasks).deleteAllTasksByGroupId(10L);
        order.verify(groups).delete(group);
    }

    @Test
    void memberCannotDeleteGroup() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new DeleteGroupCommand(2L, 10L)));
        verify(groups, never()).delete(any());
        verifyNoInteractions(tasks);
    }

    @Test
    void deletingUnknownGroupIs404() {
        when(groups.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new DeleteGroupCommand(1L, 99L)));
    }

    // ---- Quitar integrante (HUM-05): el líder expulsa, un miembro puede salir
    @Test
    void leaderRemovesMemberWithTheirTasks() {
        var membership = group.getUserProfiles().get(1);
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(membership));

        service.handle(new RemoveMemberUserFromGroupCommand(1L, 2L, 10L));

        var order = inOrder(tasks, profiles);
        order.verify(tasks).deleteTasksByUserIdAndGroupId(2L, 10L);
        order.verify(profiles).delete(membership);
        assertFalse(group.getUserProfiles().contains(membership));
        assertFalse(member.getGroupProfiles().contains(membership));
    }

    @Test
    void memberLeavesTheGroup() {
        var membership = group.getUserProfiles().get(1);
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(membership));
        service.handle(new RemoveMemberUserFromGroupCommand(2L, 2L, 10L));
        verify(profiles).delete(membership);
    }

    @Test
    void memberCannotRemoveSomeoneElse() {
        var other = user(3, "luis");
        GroupFixtures.join(group, other, GroupRole.GROUP_MEMBER);
        when(profiles.findByGroup_IdAndUser_Id(10L, 3L))
                .thenReturn(Optional.of(group.getUserProfiles().get(2)));
        assertThrows(
                AccessDeniedException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(2L, 3L, 10L)));
        verify(profiles, never()).delete(any());
        verifyNoInteractions(tasks);
    }

    @Test
    void removingSomeoneOutsideTheGroupIs404() {
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L)).thenReturn(Optional.empty());
        assertThrows(
                UserNotInGroupException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(1L, 2L, 10L)));
    }

    @Test
    void leaderCannotBeRemoved() {
        when(profiles.findByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(group.getUserProfiles().getFirst()));
        assertEquals(GroupRole.GROUP_LEADER, group.getUserProfiles().getFirst().getRoleInGroup());
        assertThrows(
                LeaderCannotBeRemovedException.class,
                () -> service.handle(new RemoveMemberUserFromGroupCommand(1L, 1L, 10L)));
        verifyNoInteractions(tasks);
    }

    // AUDIT BUG-10 (resuelto)
    @Test
    void removingLeaderIsABusinessRuleViolation() {
        when(profiles.findByGroup_IdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(group.getUserProfiles().getFirst()));
        var e = assertThrows(
                RuntimeException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(1L, 1L, 10L)));
        assertInstanceOf(BusinessRuleViolationException.class, e);
    }

    // ---- Unirse por código (HUM-05)
    @Test
    void joiningByCodeCreatesAMembershipAndConsumesPendingInvitations() {
        var newcomer = user(3, "luis");
        var pending = GroupFixtures.invitation(50, newcomer, group);
        when(groups.findByCode(group.getCode())).thenReturn(Optional.of(group));
        when(users.fetchUserById(3L)).thenReturn(Optional.of(newcomer));
        when(invitations.findByUser_IdAndGroup_Id(3L, 10L)).thenReturn(java.util.List.of(pending));

        var joined = service.handle(new JoinGroupByCodeCommand(group.getCode().code(), 3L));

        assertSame(group, joined);
        assertEquals(3, group.getMemberCount());
        verify(profiles).save(argThat(p -> p.getUser() == newcomer && p.getRoleInGroup() == GroupRole.GROUP_MEMBER));
        verify(invitations).deleteAll(java.util.List.of(pending));
    }

    @Test
    void joiningTwiceIsAConflict() {
        when(groups.findByCode(group.getCode())).thenReturn(Optional.of(group));
        when(profiles.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(true);
        assertThrows(
                UserAlreadyInGroupException.class,
                () -> service.handle(new JoinGroupByCodeCommand(group.getCode().code(), 2L)));
        verify(profiles, never()).save(any());
    }

    @Test
    void unknownCodeIs404() {
        when(groups.findByCode(any())).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class, () -> service.handle(new JoinGroupByCodeCommand("ZZZZZZZZZ", 2L)));
    }
}
