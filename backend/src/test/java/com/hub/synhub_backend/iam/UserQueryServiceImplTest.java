package com.hub.synhub_backend.iam;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.iam.application.internal.queryservices.UserQueryServiceImpl;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.queries.*;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Consultas de usuarios. */
class UserQueryServiceImplTest {

    private UserRepository users;
    private GroupProfileRepository profiles;
    private UserQueryServiceImpl service;
    private final User ana = new User("ana", "Ana", "Torres", "img", "ana@synhub.io", "hash");

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        profiles = mock(GroupProfileRepository.class);
        service = new UserQueryServiceImpl(users, profiles);
    }

    @Test void byIdUsernameAndAll() {
        when(users.findById(1L)).thenReturn(Optional.of(ana));
        when(users.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(users.findAll()).thenReturn(List.of(ana));

        assertSame(ana, service.handle(new GetUserByIdQuery(1L)).orElseThrow());
        assertSame(ana, service.handle(new GetUserByUsernameQuery("ana")).orElseThrow());
        assertEquals(List.of(ana), service.handle(new GetAllUsersQuery()));
        assertTrue(service.handle(new GetUserByUsernameQuery("nobody")).isEmpty());
    }

    @Test void usersOfAGroupComeFromItsMemberships() {
        var luis = new User("luis", "Luis", "Paz", "img", "luis@synhub.io", "hash");
        var group = new Group();
        when(profiles.findByGroup_Id(7L)).thenReturn(List.of(
                new GroupProfile(ana, group, GroupRole.GROUP_LEADER),
                new GroupProfile(luis, group, GroupRole.GROUP_MEMBER)));

        assertEquals(List.of(ana, luis), service.handle(new GetUsersByGroupIdQuery(7L)));
    }
}
