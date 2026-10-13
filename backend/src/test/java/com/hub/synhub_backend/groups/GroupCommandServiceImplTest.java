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
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Crear, editar, borrar grupos y quitar integrantes. */
class GroupCommandServiceImplTest {

    private GroupRepository groups;
    private UserRepository users;
    private GroupProfileRepository profiles;
    private TasksContextFacade tasks;
    private GroupCommandServiceImpl service;
    private User leader, member;
    private Group group;

    @BeforeEach
    void setUp() {
        groups = mock(GroupRepository.class);
        users = mock(UserRepository.class);
        profiles = mock(GroupProfileRepository.class);
        tasks = mock(TasksContextFacade.class);
        service = new GroupCommandServiceImpl(groups, users, profiles, tasks);
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

    // ---- Quitar integrante
    @Test void removesMemberFromGroup() {
        var membership = group.getUserProfiles().get(1);
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(membership));

        service.handle(new RemoveMemberUserFromGroupCommand(2L, 10L));

        assertFalse(group.getUserProfiles().contains(membership));
        assertFalse(member.getGroupProfiles().contains(membership));
    }

    @Test void removingSomeoneOutsideTheGroupIs404() {
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L)).thenReturn(Optional.empty());
        assertThrows(UserNotInGroupException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(2L, 10L)));
    }

    @Test void leaderCannotBeRemoved() {
        when(profiles.findByGroup_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(group.getUserProfiles().getFirst()));
        assertEquals(GroupRole.GROUP_LEADER, group.getUserProfiles().getFirst().getRoleInGroup());
        assertThrows(LeaderCannotBeRemovedException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(1L, 10L)));
    }

    @Disabled("AUDIT BUG-10: LeaderCannotBeRemovedException extiende RuntimeException y responde 500 en lugar de 409")
    @Test void removingLeaderIsABusinessRuleViolation() {
        when(profiles.findByGroup_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(group.getUserProfiles().getFirst()));
        var e = assertThrows(RuntimeException.class, () -> service.handle(new RemoveMemberUserFromGroupCommand(1L, 10L)));
        assertInstanceOf(BusinessRuleViolationException.class, e);
    }
}
