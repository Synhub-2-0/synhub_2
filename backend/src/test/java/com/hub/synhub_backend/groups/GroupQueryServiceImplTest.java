package com.hub.synhub_backend.groups;

import com.hub.synhub_backend.groups.application.internal.queryservices.GroupQueryServiceImpl;
import com.hub.synhub_backend.groups.domain.model.queries.*;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Consultas de grupos y membresías. */
class GroupQueryServiceImplTest {

    private GroupRepository groups;
    private GroupProfileRepository profiles;
    private GroupQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        groups = mock(GroupRepository.class);
        profiles = mock(GroupProfileRepository.class);
        service = new GroupQueryServiceImpl(groups, profiles);
    }

    @Test void groupsByRoleOnlyReturnsMembershipsWithThatRole() {
        var ana = user(2, "ana");
        var mine = group(10, ana);                 // ana lidera
        var other = group(11, user(1, "lider"), ana); // ana es miembro
        when(profiles.findByUser_Id(2L)).thenReturn(ana.getGroupProfiles());

        assertEquals(List.of(mine), service.handle(new GetGroupsByUserRoleQuery(2L, GroupRole.GROUP_LEADER)));
        assertEquals(List.of(other), service.handle(new GetGroupsByUserRoleQuery(2L, GroupRole.GROUP_MEMBER)));
        assertEquals(List.of(mine, other), service.handle(new GetGroupsByUserIdQuery(2L)));
    }

    @Test void membersOfUnknownGroupIs404() {
        when(groups.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new GetGroupMembersByGroupIdQuery(99L, 1L)));
    }

    @Test void membersOfGroup() {
        var group = group(10, user(1, "lider"), user(2, "ana"));
        when(groups.existsById(10L)).thenReturn(true);
        when(profiles.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(true);
        when(profiles.findByGroup_Id(10L)).thenReturn(group.getUserProfiles());
        assertEquals(2, service.handle(new GetGroupMembersByGroupIdQuery(10L, 2L)).size());
        assertThrows(AccessDeniedException.class, () -> service.handle(new GetGroupMembersByGroupIdQuery(10L, 99L)));
    }

    @Test void byCode() {
        var group = group(10, user(1, "lider"));
        when(groups.findByCode(new GroupCode("ABC123XYZ"))).thenReturn(Optional.of(group));
        assertSame(group, service.handle(new GetGroupByCodeQuery("ABC123XYZ")).orElseThrow());
        assertTrue(service.handle(new GetGroupByCodeQuery("ZZZ999ZZZ")).isEmpty());
    }

    @Test void malformedCodeIsABadRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.handle(new GetGroupByCodeQuery("bad")));
    }

    // ---- AUDIT SEC-01
    @Test void groupByIdIsOnlyVisibleToMembers() {
        var group = group(10, user(1, "lider"));
        when(groups.findById(10L)).thenReturn(Optional.of(group));
        when(profiles.existsByGroup_IdAndUser_Id(10L, 1L)).thenReturn(true);
        assertSame(group, service.handle(new GetGroupForMemberQuery(10L, 1L)).orElseThrow());
        assertThrows(AccessDeniedException.class, () -> service.handle(new GetGroupForMemberQuery(10L, 99L)));
        assertTrue(service.handle(new GetGroupForMemberQuery(11L, 1L)).isEmpty());
    }
}
