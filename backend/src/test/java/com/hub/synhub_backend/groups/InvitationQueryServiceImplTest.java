package com.hub.synhub_backend.groups;

import com.hub.synhub_backend.groups.application.internal.acl.GroupsContextFacadeImpl;
import com.hub.synhub_backend.groups.application.internal.queryservices.InvitationQueryServiceImpl;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByGroupIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
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

        var service = new InvitationQueryServiceImpl(repo);

        assertEquals(List.of(invitation), service.handle(new GetInvitationsByUserIdQuery(3L)));
        assertEquals(List.of(invitation), service.handle(new GetInvitationsByGroupIdQuery(10L)));
    }

    @Test void facadeReturnsLeaderIdWhenGroupHasLeader() {
        var queries = mock(GroupQueryService.class);
        when(queries.handle(new GetGroupByIdQuery(10L))).thenReturn(Optional.of(group(10, user(1, "lider"))));
        assertEquals(Optional.of(1L), new GroupsContextFacadeImpl(queries).fetchLeaderIdByGroupId(10L));
    }

    @Test void facadeReturnsEmptyWithoutLeaderOrGroup() {
        var queries = mock(GroupQueryService.class);
        when(queries.handle(new GetGroupByIdQuery(10L))).thenReturn(Optional.of(new Group("G", "d", "", GroupCode.random())));
        when(queries.handle(new GetGroupByIdQuery(99L))).thenReturn(Optional.empty());
        var facade = new GroupsContextFacadeImpl(queries);
        assertTrue(facade.fetchLeaderIdByGroupId(10L).isEmpty());
        assertTrue(facade.fetchLeaderIdByGroupId(99L).isEmpty());
    }
}
