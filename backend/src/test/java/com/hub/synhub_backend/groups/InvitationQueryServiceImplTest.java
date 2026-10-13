package com.hub.synhub_backend.groups;

import com.hub.synhub_backend.groups.application.internal.acl.GroupsContextFacadeImpl;
import com.hub.synhub_backend.groups.application.internal.queryservices.InvitationQueryServiceImpl;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByGroupIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Consultas de invitaciones y la ACL de groups. */
class InvitationQueryServiceImplTest {

    @Test void invitationsByUserAndByGroup() {
        var repo = mock(InvitationRepository.class);
        var group = group(10, user(1, "lider"));
        var invitation = invitation(50, user(3, "invitado"), group);
        when(repo.findByUser_Id(3L)).thenReturn(List.of(invitation));
        when(repo.findByGroup_Id(10L)).thenReturn(List.of(invitation));

        var groupRepo = mock(GroupRepository.class);
        when(groupRepo.findById(10L)).thenReturn(Optional.of(group));
        var service = new InvitationQueryServiceImpl(repo, groupRepo);

        assertEquals(List.of(invitation), service.handle(new GetInvitationsByUserIdQuery(3L)));
        assertEquals(List.of(invitation), service.handle(new GetInvitationsByGroupIdQuery(10L, 1L)));
        // AUDIT SEC-01: solo el líder ve las invitaciones del grupo
        assertThrows(AccessDeniedException.class, () -> service.handle(new GetInvitationsByGroupIdQuery(10L, 3L)));
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new GetInvitationsByGroupIdQuery(99L, 1L)));
    }

    @Test void facadeReturnsLeaderIdWhenGroupHasLeader() {
        var queries = mock(GroupQueryService.class);
        when(queries.handle(new GetGroupByIdQuery(10L))).thenReturn(Optional.of(group(10, user(1, "lider"))));
        assertEquals(Optional.of(1L), new GroupsContextFacadeImpl(queries, mock(GroupProfileRepository.class)).fetchLeaderIdByGroupId(10L));
    }

    @Test void facadeReturnsEmptyWithoutLeaderOrGroup() {
        var queries = mock(GroupQueryService.class);
        when(queries.handle(new GetGroupByIdQuery(10L))).thenReturn(Optional.of(new Group("G", "d", "", GroupCode.random())));
        when(queries.handle(new GetGroupByIdQuery(99L))).thenReturn(Optional.empty());
        var facade = new GroupsContextFacadeImpl(queries, mock(GroupProfileRepository.class));
        assertTrue(facade.fetchLeaderIdByGroupId(10L).isEmpty());
        assertTrue(facade.fetchLeaderIdByGroupId(99L).isEmpty());
    }

    @Test void facadeAnswersMembershipAndLeadership() {
        var profiles = mock(GroupProfileRepository.class);
        var group = group(10, user(1, "lider"), user(2, "ana"));
        when(profiles.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(true);
        when(profiles.findByGroup_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(group.getUserProfiles().get(0)));
        when(profiles.findByGroup_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(group.getUserProfiles().get(1)));
        assertEquals(GroupRole.GROUP_MEMBER, group.getUserProfiles().get(1).getRoleInGroup());
        var facade = new GroupsContextFacadeImpl(mock(GroupQueryService.class), profiles);

        assertTrue(facade.isMember(10L, 2L));
        assertFalse(facade.isMember(10L, 99L));
        assertFalse(facade.isMember(null, 2L));
        assertTrue(facade.isLeader(10L, 1L));
        assertFalse(facade.isLeader(10L, 2L));
        assertFalse(facade.isLeader(10L, 99L));
    }
}
