package com.hub.synhub_backend.iam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.iam.application.internal.acl.IamContextFacadeImpl;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.commands.SignUpCommand;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByIdQuery;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.model.valueobjects.AccountRole;
import com.hub.synhub_backend.iam.domain.services.UserCommandService;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** ACL que IAM ofrece a otros contextos. */
class IamContextFacadeImplTest {

    private UserCommandService commands;
    private UserQueryService queries;
    private IamContextFacadeImpl facade;
    private User ana;

    @BeforeEach
    void setUp() {
        commands = mock(UserCommandService.class);
        queries = mock(UserQueryService.class);
        facade = new IamContextFacadeImpl(commands, queries);
        ana = mock(User.class);
        when(ana.getId()).thenReturn(5L);
        when(ana.getUsername()).thenReturn("ana");
    }

    @Test
    void fetchUserIdByUsername() {
        when(queries.handle(new GetUserByUsernameQuery("ana"))).thenReturn(Optional.of(ana));
        when(queries.handle(new GetUserByUsernameQuery("nobody"))).thenReturn(Optional.empty());
        assertEquals(5L, facade.fetchUserIdByUsername("ana"));
        assertEquals(0L, facade.fetchUserIdByUsername("nobody"));
    }

    @Test
    void fetchUsernameByUserId() {
        when(queries.handle(new GetUserByIdQuery(5L))).thenReturn(Optional.of(ana));
        when(queries.handle(new GetUserByIdQuery(6L))).thenReturn(Optional.empty());
        assertEquals("ana", facade.fetchUsernameByUserId(5L));
        assertEquals("", facade.fetchUsernameByUserId(6L));
    }

    @Test
    void fetchUserByIdIgnoresInvalidIds() {
        assertTrue(facade.fetchUserById(null).isEmpty());
        assertTrue(facade.fetchUserById(0L).isEmpty());
        verifyNoInteractions(queries);
    }

    @Test
    void createUserUsesDefaultRole() {
        when(commands.handle(any(SignUpCommand.class))).thenReturn(Optional.of(ana));
        assertEquals(5L, facade.createUser("ana", "Ana", "Torres", "img", "ana@synhub.io", "pw"));
        var command = ArgumentCaptor.forClass(SignUpCommand.class);
        verify(commands).handle(command.capture());
        assertEquals(
                List.of(AccountRole.ROLE_USER),
                command.getValue().roles().stream().map(Role::getName).toList());
    }

    @Test
    void createUserReturnsZeroWhenItFails() {
        when(commands.handle(any(SignUpCommand.class))).thenReturn(Optional.empty());
        assertEquals(0L, facade.createUser("ana", "Ana", "Torres", "img", "ana@synhub.io", "pw", List.of("ROLE_USER")));
    }
}
