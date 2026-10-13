package com.hub.synhub_backend.groups;

import com.hub.synhub_backend.groups.application.internal.commandservices.GroupCommandServiceImpl;
import com.hub.synhub_backend.groups.domain.exceptions.LeaderCannotBeRemovedException;
import com.hub.synhub_backend.groups.domain.exceptions.UserNotInGroupException;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.commands.*;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import com.hub.synhub_backend.groups.domain.exceptions.UserAlreadyInGroupException;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** Crear, editar, borrar grupos y quitar integrantes. */
class GroupCommandServiceImplTest {

    private GroupRepository groups;
    private UserRepository users;
    private GroupProfileRepository profiles;
    private TasksContextFacade tasks;
    private InvitationRepository invitations;
    private GroupCommandServiceImpl service;
    private User leader, member;
    private Group group;

    @BeforeEach
    void setUp() {
        groups = mock(GroupRepository.class);
        users = mock(UserRepository.class);
        profiles = mock(GroupProfileRepository.class);
        tasks = mock(TasksContextFacade.class);
        invitations = mock(InvitationRepository.class);
        service = new GroupCommandServiceImpl(groups, users, profiles, tasks, invitations);
        leader = user(1, "lider");
        member = user(2, "ana");
        group = group(10, leader, member);
        when(groups.findById(10L)).thenReturn(Optional.of(group));
        when(groups.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        when(users.findById(1L)).thenReturn(Optional.of(leader));
        when(users.findById(2L)).thenReturn(Optional.of(member));
    }

    // ---- Crear
    @Test void creatorBecomesLeaderAndCodeIsRegeneratedOnCollision() {
        when(groups.existsByCode(any(GroupCode.class))).thenReturn(true, false);

        var created = service.handle(new CreateGroupCommand("Nuevo", "", "desc", 1L));

        assertSame(leader, created.getLeader());
        assertEquals(1, created.getMemberCount());
        assertTrue(created.getCode().code().matches("[0-9A-Z]{9}"));
        verify(groups, times(2)).existsByCode(any(GroupCode.class));
        verify(groups).save(created);
    }

    @Test void createWithUnknownLeaderIs404() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new CreateGroupCommand("G", "", "d", 99L)));
        verify(groups, never()).save(any());
    }

    // ---- Editar
    @Test void leaderUpdatesGroup() {
        var updated = service.handle(new UpdateGroupCommand(1L, 10L, "Renombrado", "", ""));
        assertEquals("Renombrado", updated.getName());
    }

    @Test void memberCannotUpdateGroup() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new UpdateGroupCommand(2L, 10L, "X", "", "")));
        verify(groups, never()).save(any());
    }

    // ---- Borrar
    @Test void leaderDeletesGroup() {
        service.handle(new DeleteGroupCommand(1L, 10L));
        // AUDIT BUG-02: primero las tareas (y sus requests), después el grupo
        var order = inOrder(tasks, groups);
        order.verify(tasks).deleteAllTasksByGroupId(10L);
        order.verify(groups).delete(group);
    }

    @Test void memberCannotDeleteGroup() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new DeleteGroupCommand(2L, 10L)));
        verify(groups, never()).delete(any());
        verifyNoInteractions(tasks);
    }

    @Test void deletingUnknownGroupIs404() {
        when(groups.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new DeleteGroupCommand(1L, 99L)));
    }

    // ---- Quitar integrante (HUM-05): el líder expulsa, un miembro puede salir
    @Test void leaderRemovesMemberWithTheirTasks() {
        var membership = group.getUserProfiles().get(1);
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(membership));

        service.handle(new RemoveMemberUserFromGroupCommand(1L, 2L, 10L));

        var order = inOrder(tasks, profiles);
        order.verify(tasks).deleteTasksByUserIdAndGroupId(2L, 10L);
        order.verify(profiles).delete(membership);
        assertFalse(group.getUserProfiles().contains(membership));
        assertFalse(member.getGroupProfiles().contains(membership));
    }

    @Test void memberLeavesTheGroup() {
        var membership = group.getUserProfiles().get(1);
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(membership));
        service.handle(new RemoveMemberUserFromGroupCommand(2L, 2L, 10L));
        verify(profiles).delete(membership);
    }

    @Test void memberCannotRemoveSomeoneElse() {
        var other = user(3, "luis");
        GroupFixtures.join(group, other, GroupRole.GROUP_MEMBER);
        when(profiles.findByGroup_IdAndUser_Id(10L, 3L)).thenReturn(Optional.of(group.getUserProfiles().get(2)));
        assertThrows(AccessDeniedException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(2L, 3L, 10L)));
        verify(profiles, never()).delete(any());
        verifyNoInteractions(tasks);
    }

    @Test void removingSomeoneOutsideTheGroupIs404() {
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L)).thenReturn(Optional.empty());
        assertThrows(UserNotInGroupException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(1L, 2L, 10L)));
    }

    @Test void leaderCannotBeRemoved() {
        when(profiles.findByGroup_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(group.getUserProfiles().getFirst()));
        assertEquals(GroupRole.GROUP_LEADER, group.getUserProfiles().getFirst().getRoleInGroup());
        assertThrows(LeaderCannotBeRemovedException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(1L, 1L, 10L)));
        verifyNoInteractions(tasks);
    }

    // AUDIT BUG-10 (resuelto)
    @Test void removingLeaderIsABusinessRuleViolation() {
        when(profiles.findByGroup_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(group.getUserProfiles().getFirst()));
        var e = assertThrows(RuntimeException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(1L, 1L, 10L)));
        assertInstanceOf(BusinessRuleViolationException.class, e);
    }

    // ---- Unirse por código (HUM-05)
    @Test void joiningByCodeCreatesAMembershipAndConsumesPendingInvitations() {
        var newcomer = user(3, "luis");
        var pending = GroupFixtures.invitation(50, newcomer, group);
        when(groups.findByCode(group.getCode())).thenReturn(Optional.of(group));
        when(users.findById(3L)).thenReturn(Optional.of(newcomer));
        when(invitations.findByUser_IdAndGroup_Id(3L, 10L)).thenReturn(java.util.List.of(pending));

        var joined = service.handle(new JoinGroupByCodeCommand(group.getCode().code(), 3L));

        assertSame(group, joined);
        assertEquals(3, group.getMemberCount());
        verify(profiles).save(argThat(p -> p.getUser() == newcomer && p.getRoleInGroup() == GroupRole.GROUP_MEMBER));
        verify(invitations).deleteAll(java.util.List.of(pending));
    }

    @Test void joiningTwiceIsAConflict() {
        when(groups.findByCode(group.getCode())).thenReturn(Optional.of(group));
        when(profiles.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(true);
        assertThrows(UserAlreadyInGroupException.class, () -> service.handle(new JoinGroupByCodeCommand(group.getCode().code(), 2L)));
        verify(profiles, never()).save(any());
    }

    @Test void unknownCodeIs404() {
        when(groups.findByCode(any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new JoinGroupByCodeCommand("ZZZZZZZZZ", 2L)));
    }
}
